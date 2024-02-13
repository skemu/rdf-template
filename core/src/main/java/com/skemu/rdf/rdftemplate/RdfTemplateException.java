package com.skemu.rdf.rdftemplate;

import lombok.NonNull;

public class RdfTemplateException extends RuntimeException {

    public RdfTemplateException(@NonNull String message) {
        super(message);
    }

    public RdfTemplateException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }
}
