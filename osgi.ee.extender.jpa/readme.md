#### OSGi JEE7 JPA extender
JPA extension is standardized in the OSGi enterprise standard. The solution here follows (largely) that standard by processing
bundles that have a `Meta-Persistence` header in their manifest. It registers an `EntityManagerFactory` for each persistence
unit found.

Next to that, it also registers an `EntityManager` service for each persistence unit so these can be directly injected in/used by 
SCR components. The service handles the restriction that an entity manager can only be used by one thread.

More documentation about the extender can be found [here][1].

[1]: http://www.avineas.org/uploads/jee-extender.pdf