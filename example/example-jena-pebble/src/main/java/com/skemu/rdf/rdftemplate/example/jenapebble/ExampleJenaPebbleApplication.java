package com.skemu.rdf.rdftemplate.example.jenapebble;

import com.skemu.rdf.rdftemplate.RdfTemplateProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan("com.skemu.rdf.rdftemplate")
public class ExampleJenaPebbleApplication implements CommandLineRunner {
    private final RdfTemplateProcessor rdfTemplateProcessor;

    public ExampleJenaPebbleApplication(RdfTemplateProcessor rdfTemplateProcessor) {
        this.rdfTemplateProcessor = rdfTemplateProcessor;
    }

    public static void main(String... args) {
        SpringApplication.run(ExampleJenaPebbleApplication.class, args).close();
    }

    @Override
    public void run(String... args) {
        LOG.info("Running");
        rdfTemplateProcessor.process();
        LOG.info("Done");
    }
}
