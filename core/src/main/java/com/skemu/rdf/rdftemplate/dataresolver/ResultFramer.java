package com.skemu.rdf.rdftemplate.dataresolver;

import static com.skemu.rdf.rdftemplate.collectors.Collectors.toUnmodifiableLinkedHashMap;
import static com.skemu.rdf.rdftemplate.config.DataSource.FRAME_NODE_ORDER_BY;
import static com.skemu.rdf.rdftemplate.config.DataSource.FRAME_NODE_PREFIX;
import static com.skemu.rdf.rdftemplate.config.DataSource.FRAME_NODE_TYPE;
import static com.skemu.rdf.rdftemplate.config.DataSource.FRAME_NODE_TYPE_STRING;
import static com.skemu.rdf.rdftemplate.config.DataSource.FRAME_NODE_VALUE;
import static java.util.Map.entry;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.skemu.rdf.rdftemplate.config.DataSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResultFramer {

    public static final String INVALID_RESULT_FRAME = "Invalid %s `%s` for resultFrame: %s";

    public static final String MISSING_PROP_RESULT_FRAME = "Missing %s for resultFrame: %s";

    public static Map<List<String>, Map<String, Object>> map(
            List<Map<String, String>> resolved,
            Map<String, Object> resultFrame,
            Map<String, String> namespacePrefixes) {
        var nodeKey = getNodeKey(resultFrame);

        var resultsByKey = resolved.stream()
                .filter(map -> !extractKey(nodeKey, map).isEmpty())
                .collect(groupingBy(map -> extractKey(nodeKey, map), LinkedHashMap::new, toList()));

        var orderBy = getOrderBy(resultFrame);

        var unorderedResults = resultsByKey.entrySet().stream()
                .map(rowGroupEntry -> entry(
                        rowGroupEntry.getKey(),
                        resultFrame.entrySet().stream()
                                .map(entry ->
                                        mapResultFrameProperty(rowGroupEntry.getValue(), entry, namespacePrefixes))
                                .filter(Objects::nonNull)
                                .collect(toUnmodifiableLinkedHashMap(Entry::getKey, Entry::getValue))));

        if (orderBy != null) {
            return unorderedResults
                    .sorted(orderBy)
                    .collect(toUnmodifiableLinkedHashMap(Entry::getKey, Entry::getValue));
        }

        return unorderedResults.collect(toUnmodifiableLinkedHashMap(Entry::getKey, Entry::getValue));
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
            } else if (frameNode instanceof Map<?, ?>) {
                var valueMappingNode = (Map<String, Object>) frameNode;

                valueExpression = getFrameNodePropertyValue(FRAME_NODE_VALUE, valueMappingNode);
                valueType =
                        getFrameNodePropertyValueOrDefault(FRAME_NODE_TYPE, valueMappingNode, FRAME_NODE_TYPE_STRING);
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
            String property, Map<String, Object> frameNode, String defaultResult) {
        var result = getFrameNodePropertyValue(property, frameNode, true);

        if (result == null) {
            return defaultResult;
        }

        return result;
    }

    private static String getFrameNodePropertyValue(String property, Map<String, Object> frameNode) {
        return getFrameNodePropertyValue(property, frameNode, false);
    }

    private static String getFrameNodePropertyValue(String property, Map<String, Object> frameNode, boolean nullable) {
        var propertyValue = frameNode.get(property);

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

    private static Comparator<Entry<List<String>, Map<String, Object>>> getOrderBy(Map<String, Object> frameNode) {
        var orderBy = getFrameNodePropertyValue(FRAME_NODE_ORDER_BY, frameNode, true);

        if (orderBy == null) {
            return null;
        }

        if (orderBy.startsWith("-")) {
            return Comparator.comparing(
                            (Entry<List<String>, Map<String, Object>> entry) ->
                                    getFrameNodePropertyValue(orderBy.substring(1), entry.getValue()),
                            String.CASE_INSENSITIVE_ORDER)
                    .reversed();
        } else if (orderBy.startsWith("+")) {
            return Comparator.comparing(
                    (Entry<List<String>, Map<String, Object>> entry) ->
                            getFrameNodePropertyValue(orderBy.substring(1), entry.getValue()),
                    String.CASE_INSENSITIVE_ORDER);
        } else {
            throw new DataResolverException(String.format("%s should  start with '+' or '-'", FRAME_NODE_ORDER_BY));
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
                // TODO introduce strategy for handling multiple values?
                // return String.join(", ", expressionResult);
                return expressionResult.getFirst();
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
        return row.entrySet().stream()
                .filter(entry -> key.contains(entry.getKey()))
                .map(Entry::getValue)
                .toList();
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
