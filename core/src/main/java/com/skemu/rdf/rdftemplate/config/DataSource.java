package com.skemu.rdf.rdftemplate.config;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Builder(toBuilder = true)
@Jacksonized
@Getter
public class DataSource {

    public static final String VAR_NODE_KEY = "_key";

    public static final String VAR_NODE_VALUE = "_value";

    public static final String VAR_NODE_TYPE = "_type";

    public static final String VAR_NODE_TYPE_SET = "set";

    public static final String VAR_NODE_TYPE_STRING = "string";

    private String name;

    private String resolver;

    private String location;

    private String source;

    @Builder.Default
    private Map<String, Object> resultFrame = Map.of();
}
