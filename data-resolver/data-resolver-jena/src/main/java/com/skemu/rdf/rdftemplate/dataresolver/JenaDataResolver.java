package com.skemu.rdf.rdftemplate.dataresolver;

import com.skemu.rdf.rdftemplate.config.ConfigResourceLoaders;
import com.skemu.rdf.rdftemplate.config.DataSource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.ModelCollector.UnionModelCollector;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class JenaDataResolver implements DataSourceResolver {
    @Override
    public String getId() {
        // TODO: make configurable via props
        return "sparql";
    }

    @Override
    public List<Map<String, String>> resolve(DataSource dataSource) {
        Model model = null;
        var dataSourceSources = dataSource.getSources();

        if (!dataSourceSources.isEmpty()) {
            model = toSourceResources(dataSourceSources)
                    .map(ConfigResourceLoaders::resolveResourceUriStringsDeeply)
                    .flatMap(List::stream)
                    .map(RDFDataMgr::loadModel)
                    .collect(new UnionModelCollector());
        } else {
            throw new DataResolverException(String.format("No source found for data source %s", dataSource.getName()));
        }

        var query = ConfigResourceLoaders.getResource(dataSource.getLocation())
                .map(ConfigResourceLoaders::resolveResourceUriString)
                .map(QueryFactory::read)
                .orElseThrow(() ->
                        new DataResolverException(String.format("Could not find query %s", dataSource.getLocation())));

        return executeQuery(model, query);
    }

    private Stream<Resource> toSourceResources(Set<String> sources) {
        return sources.stream()
                .map(source -> {
                    var sourceResource = ConfigResourceLoaders.getResource(source);

                    if (sourceResource.isEmpty()) {
                        throw new DataResolverException(String.format("Could not find source %s", source));
                    }

                    return sourceResource;
                })
                .flatMap(Optional::stream);
    }

    private List<Map<String, String>> executeQuery(Model model, Query query) {
        List<Map<String, String>> result = new ArrayList<>();
        try (QueryExecution queryExecution = QueryExecutionFactory.create(query, model)) {
            ResultSet resultSet = queryExecution.execSelect();

            while (resultSet.hasNext()) {
                QuerySolution querySolution = resultSet.nextSolution();
                Iterator<String> varNames = querySolution.varNames();
                Map<String, String> row = new LinkedHashMap<>();
                while (varNames.hasNext()) {
                    String name = varNames.next();
                    row.put(name, querySolution.get(name).toString());
                }

                result.add(row);
            }
        }

        return result;
    }
}
