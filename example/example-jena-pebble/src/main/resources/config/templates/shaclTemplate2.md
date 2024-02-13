# Klassen

{% set nodeShape = nodeShapes[["http://www.w3.org/ns/dcat#Dataset"]] %}
## Klasse {{ nodeShape.targetClass }}.

| Property                                     | URI                       | Range                | Card        |
|----------------------------------------------|---------------------------|----------------------|-------------|
{% for propEntry in nodeShape.propertyShapes %}
{% set prop = propEntry.value %}
| [{{ prop.propertyName }}]({{ prop.property }}) |  {{ prop.property }} | {% if prop.class != null %} {{ prop.class }} {% elseif prop.datatype != null %} {{ prop.datatype }} {% elseif prop.nodeKind != null %} {{ prop.nodeKind }}{% endif %} | {{ prop.minCount | default("0") }}..{{ prop.maxCount | default("n") }} |
{% endfor %}
