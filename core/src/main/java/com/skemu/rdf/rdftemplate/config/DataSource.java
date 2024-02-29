package com.skemu.rdf.rdftemplate.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
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

    public static final String FRAME_NODE_ORDER_BY = "_orderBy";

    private String name;

    private String resolver;

    private String location;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonProperty("source")
    @Singular
    private Set<String> sources;

    @Builder.Default
    private Map<String, Object> resultFrame = Map.of();
}
