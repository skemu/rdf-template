package com.skemu.rdf.rdftemplate.dataresolver;

import static com.skemu.rdf.rdftemplate.config.DataSource.VAR_NODE_TYPE;
import static com.skemu.rdf.rdftemplate.config.DataSource.VAR_NODE_TYPE_STRING;
import static com.skemu.rdf.rdftemplate.config.DataSource.VAR_NODE_VALUE;
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

    public static Map<List<String>, Object> map(
            List<Map<String, String>> resolved, Map<String, Object> varMapping, Map<String, String> namespacePrefixes) {
        var nodeKey = getNodeKey(varMapping);

        var resultsByKey = resolved.stream()
                .filter(map -> !extractKey(nodeKey, map).isEmpty())
                .collect(groupingBy(map -> extractKey(nodeKey, map), toList()));

        return resultsByKey.entrySet().stream()
                .map(rowGroupEntry -> entry(
                        rowGroupEntry.getKey(),
                        varMapping.entrySet().stream()
                                .map(entry -> mapVarMappingProperty(rowGroupEntry.getValue(), entry, namespacePrefixes))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue))))
                .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));
    }

    @SuppressWarnings("unchecked")
    private static Entry<String, Object> mapVarMappingProperty(
            List<Map<String, String>> rowGroup, Entry<String, Object> entry, Map<String, String> namespacePrefixes) {
        var varValue = entry.getValue();
        if (varValue instanceof Map<?, ?> varMappingNode && varMappingNode.containsKey(DataSource.VAR_NODE_KEY)) {
            return entry(entry.getKey(), map(rowGroup, (Map<String, Object>) varMappingNode, namespacePrefixes));

        } else {
            String valueExpression;
            String valueType;
            if (varValue instanceof String expr) {
                valueExpression = expr;
                valueType = VAR_NODE_TYPE_STRING;
            } else if (varValue instanceof Map<?, ?> valueMappingNode) {
                valueExpression = (String) valueMappingNode.get(VAR_NODE_VALUE);
                valueType = (String) valueMappingNode.get(VAR_NODE_TYPE);
            } else {
                throw new DataResolverException(String.format("Invalid varMapping: %s", varValue));
            }

            if (valueExpression == null) {
                throw new DataResolverException(
                        String.format("Missing %s for varMapping: %s", VAR_NODE_VALUE, varValue));
            }
            if (valueType == null) {
                throw new DataResolverException(
                        String.format("Missing %s for varMapping: %s", VAR_NODE_TYPE, varValue));
            }

            var resolvedExpr = resolvePropertyExpression(valueExpression, rowGroup, namespacePrefixes, valueType);
            if (resolvedExpr != null) {
                return entry(entry.getKey(), resolvedExpr);
            }
        }

        return null;
    }

    private static Object resolvePropertyExpression(
            String expr, List<Map<String, String>> rowGroup, Map<String, String> namespacePrefixes, String valueType) {
        // TODO prefix

        var expressionResult = rowGroup.stream() //
                .map(row -> row.get(expr))
                .distinct()
                .filter(Objects::nonNull)
                .toList();

        if (valueType.equals(VAR_NODE_TYPE_STRING)) {
            if (expressionResult.isEmpty()) {
                return null;
            } else {
                return String.join(", ", expressionResult);
            }
        }
        return expressionResult;
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

    private static List<String> getNodeKey(Map<String, Object> varMappingNode) {
        var nodeId = varMappingNode.get(DataSource.VAR_NODE_KEY);

        // TODO check wellformedness of varMapping in core

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
