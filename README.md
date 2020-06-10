# `IU_IDSL_JENA`

Tools for use with Apache Jena. Apache Jena is 
"A free and open source Java framework for building Semantic Web and
Linked Data applications."

* [Apache Jena](https://jena.apache.org/)
* [Apache Jena Javadocs](https://jena.apache.org/documentation/javadoc/jena/index.html)

## Dependencies

* Java 1.8+
* [IU\_IDSL\_UTIL](https://github.com/IUIDSL/iu_idsl_jena)
* Jackson-Core, Jackson-Databind

## Compiling

```
mvn clean install
```

## Usage

Example

```
java -jar iu_idsl_jena-0.0.1-SNAPSHOT-jar-with-dependencies.jar -ontfile efo.owl -vv -ont2tsv -o efo.tsv
```
