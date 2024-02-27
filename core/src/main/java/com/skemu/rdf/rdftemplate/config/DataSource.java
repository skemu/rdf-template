package com.skemu.rdf.rdftemplate.config;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Builder(toBuilder = true)
@Jacksonized
@Getter
@ToString
public class DataSource {

    public static final String FRAME_NODE_KEY = "_key";

    public static final String FRAME_NODE_VALUE = "_value";

    public static final String FRAME_NODE_TYPE = "_type";

    public static final String FRAME_NODE_TYPE_SET = "set";

    public static final String FRAME_NODE_TYPE_STRING = "string";

    public static final String FRAME_NODE_PREFIX = "_prefix";

    private String name;

    private String resolver;

    private String location;

    private String source;

    @Builder.Default
    private Map<String, Object> resultFrame = Map.of();
}
