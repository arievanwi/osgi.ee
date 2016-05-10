#### Hibernate persistence provider exporter
The purpose of this bundle is to export a standard JEE [persistence provider][1] for Hibernate. Although Hibernate itself does support OSGi,
it does not set the correct properties on the service, hence this adapter. Furthermore, this adapter also exports the dom4j packages needed
by Hibernate since the dom4j bundles from various sources need stax at a specific version (which is not needed on Java 8). 

[1]: http://docs.oracle.com/javaee/7/api/javax/persistence/spi/PersistenceProvider.html