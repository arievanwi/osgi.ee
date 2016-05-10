#### Hibernate persistence provider exporter
The purpose of this bundle is to export a standard JEE [persistence provider][1] for Hibernate. Although Hibernate itself does support OSGi,
it does not set the correct properties on the service, hence this adapter. Furthermore, this adapter also exports the dom4j packages needed
by Hibernate since the dom4j bundles from various sources need stax at a specific version (which is not needed on Java 8). 

Note that the Hibernate OSGi remarks for the needed imports (org.hibernate.proxy, javassist.util.proxy) also apply here since the
proxy creation of Hibernate *forces* that these packages are visible from the class loader that loads the entity classes 
(which is braindead IMHO given the fact that all kinds of mechanisms exist to pass class loaders but the proxy creation doesn't use them).

To get the adapter to work, you will need, next to the required bundles of Hibernate, the JPA bundle (hibernate-entitymanager) and the OSGi bundle
(hibernate-osgi).
Checked with Hibernate 5.1.0.

[1]: http://docs.oracle.com/javaee/7/api/javax/persistence/spi/PersistenceProvider.html