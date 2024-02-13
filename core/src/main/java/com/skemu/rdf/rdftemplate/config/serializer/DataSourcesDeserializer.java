package com.skemu.rdf.rdftemplate.config.serializer;

import static com.skemu.rdf.rdftemplate.config.YamlConfigReader.DATA_SOURCES_KEY;
import static com.skemu.rdf.rdftemplate.config.YamlConfigReader.INVALID_OBJECT_NODE;
import static java.util.Collections.unmodifiableSet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skemu.rdf.rdftemplate.config.DataSource;
import com.skemu.rdf.rdftemplate.config.YamlConfigReaderException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DataSourcesDeserializer extends JsonDeserializer<Set<DataSource>> {

    @Override
    public Set<DataSource> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        var node = parser.readValueAsTree();

        if (node instanceof ObjectNode objectNode) {
            var dataSources = new HashSet<DataSource>();

            objectNode.fields().forEachRemaining(entry -> {
                try {
                    dataSources.add(context.readTreeAsValue(entry.getValue(), DataSource.class).toBuilder()
                            .name(entry.getKey())
                            .build());
                } catch (IOException e) {
                    throw new YamlConfigReaderException(
                            String.format("Could not parse data source: %s", entry.getKey()), e);
                }
            });

            return unmodifiableSet(dataSources);
        }

        throw new YamlConfigReaderException(String.format(INVALID_OBJECT_NODE, DATA_SOURCES_KEY));
    }
}
