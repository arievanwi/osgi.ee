#### Hibernate persistence provider exporter
The purpose of this bundle is to export a standard JEE [persistence provider][1] for Hibernate. Although Hibernate itself does support OSGi,
it has some flaws:
- It does not  set the correct properties on the service.
- It forces the persistence bundle to import packages (org.hibernate.proxy, javassist.util.proxy) since these packages must be visible from
the bundle class loader.

These issues are automatically resolved by this extender. Furthermore, the extender exports the dom4j packages needed
by Hibernate since the dom4j bundles from various sources need stax at a specific version (which is not needed on Java 8). 

To get the adapter to work, you will need, next to the required bundles of Hibernate, the JPA bundle (hibernate-entitymanager) and the OSGi bundle
(hibernate-osgi). Code is checked against Hibernate 5.1.0.

[1]: http://docs.oracle.com/javaee/7/api/javax/persistence/spi/PersistenceProvider.html
