# Templating Example


## Concept example

{% for conceptEntry in concepts %}
{% set concept = conceptEntry.value %}

### {{ concept.title }}

| property             | value                   |
|----------------------|-------------------------|
| **preferred label**  | {{ concept.prefLabel }} |
{% if concept.altLabel is not empty %}
| **alternative label** | {{ concept.altLabel   | join(', ') }} |
{% endif %}
| **definition**        | {{ concept.definition | join(', ') }} |
{% if concept.broaderConcept is not empty %}
| **broader concept**   | {% for item in concept.broaderConcept %}[{{ item | split("/") | last}}](#{{item | split("/") | last | lower}}){% if item != concept.broaderConcept | last %}, {% endif %}{% endfor %}|
{% endif %}

{% endfor %}

## Collection example

{% for collectionEntry in collections %}
{% set collection = collectionEntry.value %}

### {{ collection.label }}

| code                  | definition              |
|-----------------------|-------------------------|
{% for memberEntry in collection.members %}
{% set member = memberEntry.value %}
| {{ member.notation }} | {{ member.definition }} |
{% endfor %}

{% endfor %}
