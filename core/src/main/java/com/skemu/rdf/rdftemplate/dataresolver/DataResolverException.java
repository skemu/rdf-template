package com.skemu.rdf.rdftemplate.dataresolver;

import lombok.NonNull;

public class DataResolverException extends RuntimeException {

    public DataResolverException(@NonNull String message) {
        super(message);
    }

    public DataResolverException(@NonNull String message, Throwable cause) {
        super(message, cause);
    }
}
