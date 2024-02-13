package com.skemu.rdf.rdftemplate.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.skemu.rdf.rdftemplate.config.serializer.DataSourcesDeserializer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@Getter
public class RdfTemplateConfig {

    private Map<String, String> namespacePrefixes;

    @JsonDeserialize(using = DataSourcesDeserializer.class)
    private Set<DataSource> dataSources;

    private List<Template> templates;
}
