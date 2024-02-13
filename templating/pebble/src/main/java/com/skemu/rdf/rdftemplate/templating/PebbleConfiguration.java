package com.skemu.rdf.rdftemplate.templating;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.PebbleEngine.Builder;
import io.pebbletemplates.pebble.extension.Extension;
import io.pebbletemplates.pebble.loader.FileLoader;
import io.pebbletemplates.pebble.loader.Loader;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PebbleConfiguration {

    private final List<Extension> extensions;

    public PebbleConfiguration(List<Extension> extensions) {
        this.extensions = extensions;
    }

    private Loader<?> getTemplateLoader() {
        return new FileLoader();
    }

    @Bean
    public PebbleEngine pebbleEngine() {
        return new Builder()
                .extension(extensions.toArray(new Extension[0]))
                .loader(getTemplateLoader())
                .build();
    }
}
