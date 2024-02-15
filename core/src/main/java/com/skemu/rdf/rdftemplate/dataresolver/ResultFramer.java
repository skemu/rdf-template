package com.skemu.rdf.rdftemplate.dataresolver;

import static com.skemu.rdf.rdftemplate.config.DataSource.FRAME_NODE_PREFIX;
import static com.skemu.rdf.rdftemplate.config.DataSource.FRAME_NODE_TYPE;
import static com.skemu.rdf.rdftemplate.config.DataSource.FRAME_NODE_TYPE_STRING;
import static com.skemu.rdf.rdftemplate.config.DataSource.FRAME_NODE_VALUE;
import static java.util.Map.entry;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.skemu.rdf.rdftemplate.config.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResultFramer {

    public static final String INVALID_RESULT_FRAME = "Invalid %s `%s` for resultFrame: %s";

    public static final String MISSING_PROP_RESULT_FRAME = "Missing %s for resultFrame: %s";

    public static Map<List<String>, Object> map(
            List<Map<String, String>> resolved,
            Map<String, Object> resultFrame,
            Map<String, String> namespacePrefixes) {
        var nodeKey = getNodeKey(resultFrame);

        var resultsByKey = resolved.stream()
                .filter(map -> !extractKey(nodeKey, map).isEmpty())
                .collect(groupingBy(map -> extractKey(nodeKey, map), toList()));

        return resultsByKey.entrySet().stream()
                .map(rowGroupEntry -> entry(
                        rowGroupEntry.getKey(),
                        resultFrame.entrySet().stream()
                                .map(entry ->
                                        mapResultFrameProperty(rowGroupEntry.getValue(), entry, namespacePrefixes))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue))))
                .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));
    }

    @SuppressWarnings("unchecked")
    private static Entry<String, Object> mapResultFrameProperty(
            List<Map<String, String>> rowGroup, Entry<String, Object> entry, Map<String, String> namespacePrefixes) {
        var frameNode = entry.getValue();
        if (frameNode instanceof Map<?, ?> resultFrameNode && resultFrameNode.containsKey(DataSource.FRAME_NODE_KEY)) {
            return entry(entry.getKey(), map(rowGroup, (Map<String, Object>) resultFrameNode, namespacePrefixes));

        } else {
            String valueExpression;
            String valueType;
            boolean prefix = false;
            if (frameNode instanceof String expr) {
                valueExpression = expr;
                valueType = FRAME_NODE_TYPE_STRING;
            } else if (frameNode instanceof Map<?, ?> valueMappingNode) {
                valueExpression = getFrameNodePropertyValue(valueMappingNode.get(FRAME_NODE_VALUE), frameNode);
                valueType = getFrameNodePropertyValueOrDefault(
                        valueMappingNode.get(FRAME_NODE_TYPE), frameNode, FRAME_NODE_TYPE_STRING);
                var prefixValue = valueMappingNode.get(FRAME_NODE_PREFIX);
                prefix = switch (prefixValue) {
                    case null -> false;
                    case Boolean prefixBoolean -> prefixBoolean;
                    default -> throw new DataResolverException(
                            String.format(INVALID_RESULT_FRAME, FRAME_NODE_PREFIX, prefixValue, frameNode));};
            } else {
                throw new DataResolverException(String.format("Invalid resultFrame: %s", frameNode));
            }

            var resolvedExpr =
                    resolvePropertyExpression(valueExpression, rowGroup, namespacePrefixes, valueType, prefix);
            if (resolvedExpr != null) {
                return entry(entry.getKey(), resolvedExpr);
            }
        }

        return null;
    }

    private static String getFrameNodePropertyValueOrDefault(
            Object propertyValue, Object frameNode, String defaultResult) {
        var result = getFrameNodePropertyValue(propertyValue, frameNode, true);

        if (result == null) {
            return defaultResult;
        }

        return result;
    }

    private static String getFrameNodePropertyValue(Object propertyValue, Object frameNode) {
        return getFrameNodePropertyValue(propertyValue, frameNode, false);
    }

    private static String getFrameNodePropertyValue(Object propertyValue, Object frameNode, boolean nullable) {
        if (propertyValue instanceof String value) {
            return value;
        } else if (propertyValue == null) {
            if (nullable) {
                return null;
            }

            throw new DataResolverException(String.format(MISSING_PROP_RESULT_FRAME, FRAME_NODE_VALUE, frameNode));
        } else {
            throw new DataResolverException(
                    String.format(INVALID_RESULT_FRAME, FRAME_NODE_VALUE, propertyValue, frameNode));
        }
    }

    private static Object resolvePropertyExpression(
            String expr,
            List<Map<String, String>> rowGroup,
            Map<String, String> namespacePrefixes,
            String valueType,
            boolean prefix) {

        var expressionResult = rowGroup.stream()
                .map(row -> row.get(expr))
                .distinct()
                .filter(Objects::nonNull)
                .map(value -> prefix ? applyNamespacePrefix(value, namespacePrefixes) : value)
                .toList();

        if (valueType.equals(FRAME_NODE_TYPE_STRING)) {
            if (expressionResult.isEmpty()) {
                return null;
            } else {
                return String.join(", ", expressionResult);
            }
        }
        return expressionResult;
    }

    private static String applyNamespacePrefix(String value, Map<String, String> namespacePrefixes) {
        return namespacePrefixes.entrySet().stream()
                .filter(entry -> value.startsWith(entry.getValue()))
                .map(entry ->
                        entry.getKey() + ":" + value.substring(entry.getValue().length()))
                .findFirst()
                .orElse(value);
    }

    private static List<String> extractKey(List<String> key, Map<String, String> row) {
        var extractedKey = row.entrySet().stream()
                .filter(entry -> key.contains(entry.getKey()))
                .map(Entry::getValue)
                .toList();

        if (extractedKey.isEmpty()) {
            return extractedKey;
            //            throw new RdfTemplateException(String.format("Could not find key %s in result set.", key));
        }

        return extractedKey;
    }

    private static List<String> getNodeKey(Map<String, Object> resultFrameNode) {
        var nodeId = resultFrameNode.get(DataSource.FRAME_NODE_KEY);

        // TODO check wellformedness of resultFrame in core

        List<String> nodeIds = new ArrayList<>();

        if (nodeId instanceof String nodeIdString) {
            nodeIds.add(nodeIdString);
        } else if (nodeId instanceof List<?> nodeIdList) {
            nodeIdList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .forEach(nodeIds::add);
        }

        return nodeIds;
    }
}
