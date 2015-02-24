#### DataSource factory
Bundle that helps in creating `javax.sql.DataSource` instances using a configuration manager only. The
data sources can be both JTA/XA data sources as well as "normal" data sources, depending on the factory to use: 
if the factory pid is set to "XAdatasource", the datasource will work with a transaction manager, if the factory
pid is "datasource" it will be a plain old data source. Usage is further described in the user manual of
this repository.
