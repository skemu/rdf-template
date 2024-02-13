package com.skemu.rdf.rdftemplate;

import com.skemu.rdf.rdftemplate.config.ConfigProperties;
import com.skemu.rdf.rdftemplate.config.ConfigResourceLoaders;
import com.skemu.rdf.rdftemplate.config.RdfTemplateConfig;
import com.skemu.rdf.rdftemplate.config.YamlConfigReader;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RdfTemplateConfiguration {

    @Bean
    public RdfTemplateConfig rdfTemplateConfig(YamlConfigReader yamlConfigReader) {
        return ConfigResourceLoaders.getResource(ConfigProperties.CONFIG_FILE_NAME)
                .map(config -> {
                    try {
                        return yamlConfigReader.parseYamlConfig(config.getInputStream(), RdfTemplateConfig.class);
                    } catch (IOException ioException) {
                        throw new RdfTemplateException(
                                String.format("Exception while parsing %s", ConfigProperties.CONFIG_FILE_NAME),
                                ioException);
                    }
                })
                .orElseThrow(() ->
                        new RdfTemplateException(String.format("Could not find %s", ConfigProperties.CONFIG_FILE_NAME)))
                .orElseThrow(() -> new RdfTemplateException(
                        String.format("Could not parse %s", ConfigProperties.CONFIG_FILE_NAME)));
    }
}
