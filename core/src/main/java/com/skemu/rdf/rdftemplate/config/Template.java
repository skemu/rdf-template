package com.skemu.rdf.rdftemplate.config;

import java.nio.file.Path;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@Getter
@ToString
public class Template {

    public static final String TEMPLATE_DIR = "templates";

    private String templateLocation;

    private Path outputLocation;
}
