package com.skemu.rdf.rdftemplate;

import static com.skemu.rdf.rdftemplate.collectors.Collectors.toUnmodifiableLinkedHashMap;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.skemu.rdf.rdftemplate.config.ConfigResourceLoaders;
import com.skemu.rdf.rdftemplate.config.DataSource;
import com.skemu.rdf.rdftemplate.config.RdfTemplateConfig;
import com.skemu.rdf.rdftemplate.config.Template;
import com.skemu.rdf.rdftemplate.dataresolver.DataSourceResolver;
import com.skemu.rdf.rdftemplate.dataresolver.ResultFramer;
import com.skemu.rdf.rdftemplate.templating.Templater;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RdfTemplateProcessor {
    private final RdfTemplateConfig rdfTemplateConfig;

    private final Map<String, DataSourceResolver> dataSourceResolverRegistry;

    private final Templater templater;

    public RdfTemplateProcessor(
            RdfTemplateConfig rdfTemplateConfig, Set<DataSourceResolver> dataSourceResolvers, Templater templater) {
        this.rdfTemplateConfig = rdfTemplateConfig;
        this.dataSourceResolverRegistry = getDataSourceResolverRegistry(dataSourceResolvers);
        this.templater = templater;
    }

    private Map<String, DataSourceResolver> getDataSourceResolverRegistry(Set<DataSourceResolver> dataSourceResolvers) {
        return dataSourceResolvers.stream()
                .collect(
                        groupingBy(DataSourceResolver::getId, collectingAndThen(toList(), this::ensureSingleResolver)));
    }

    private DataSourceResolver ensureSingleResolver(List<DataSourceResolver> dataSourceResolvers) {
        if (dataSourceResolvers.size() > 1) {
            throw new RdfTemplateException(String.format(
                    "Expecting one, but found multiple data source resolvers for id %s: %s",
                    dataSourceResolvers.getFirst().getId(), dataSourceResolvers));
        }

        return dataSourceResolvers.getFirst();
    }

    public void process() {
        Map<String, Object> dataContext = rdfTemplateConfig.getDataSources().stream()
                .map(dataSource -> resolveForContext(dataSource, rdfTemplateConfig.getNamespacePrefixes()))
                .collect(toUnmodifiableLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

        rdfTemplateConfig.getTemplates().forEach(template -> processTemplate(template, dataContext));
    }

    private Map.Entry<String, Object> resolveForContext(DataSource dataSource, Map<String, String> namespacePrefixes) {
        var resolved = dataSourceResolverRegistry.get(dataSource.getResolver()).resolve(dataSource);

        if (resolved.isEmpty()) {
            LOG.info("No data found for data source {}. Returning empty result.", dataSource.getName());
            return Map.entry(dataSource.getName(), List.of());
        }

        Object dataSourceResult;
        if (!dataSource.getResultFrame().isEmpty()) {
            dataSourceResult = ResultFramer.map(resolved, dataSource.getResultFrame(), namespacePrefixes);
        } else {
            dataSourceResult = resolved;
        }

        return Map.entry(dataSource.getName(), dataSourceResult);
    }

    private void processTemplate(Template template, Map<String, Object> dataContext) {
        ConfigResourceLoaders.getResource(String.format("%s/%s", Template.TEMPLATE_DIR, template.getTemplateLocation()))
                .ifPresentOrElse(
                        templateResource -> evaluateAndWriteTemplate(template, templateResource, dataContext), () -> {
                            throw new RdfTemplateException(
                                    String.format("Could not locate template for template config %s", template));
                        });
    }

    private void evaluateAndWriteTemplate(
            Template template, Resource templateResource, Map<String, Object> dataContext) {
        try {
            Files.createDirectories(template.getOutputLocation());
        } catch (IOException ioException) {
            throw new RdfTemplateException(
                    String.format("Error creating output directory %s", template.getOutputLocation()), ioException);
        }

        var outputPath = template.getOutputLocation().resolve(template.getTemplateLocation());

        try (var writer = Files.newBufferedWriter(outputPath)) {
            templater.evaluateAndWrite(templateResource.getFile(), writer, dataContext);
        } catch (IOException ioException) {
            throw new RdfTemplateException(
                    String.format("An exception occurred while evaluating template %s", template), ioException);
        }
    }
}
