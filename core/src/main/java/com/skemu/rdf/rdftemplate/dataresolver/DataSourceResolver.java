package com.skemu.rdf.rdftemplate.dataresolver;

import com.skemu.rdf.rdftemplate.config.DataSource;
import java.util.List;
import java.util.Map;

public interface DataSourceResolver {

    String getId();

    List<Map<String, String>> resolve(DataSource dataSource);
}
