#### OSGi JEE7 extender 
Getting JEE7 to work in OSGi can be a real pain: there are a lot of (open source) projects going on to implement more or less the 
[OSGi R5 enterprise standard][1], but to integrate them is problematic. Not the least because of the dependencies that
are required for some projects and mismatches in implementation versions.

The extender here is meant as a one-stop solution for the following JEE7 standards on OSGi:
* CDI 2.1. Implemented using the [Weld][2] CDI reference implementation, that can be downloaded [here][3].
* JPA 2.1. Implemented using the standard JPA 2.1 interfaces only and tested with Eclipselink.
* JTA 1.1. The bundle contains a transaction manager that can be used in local solutions in case of the absence of a genuine transaction manager.
* JSF 2.1. The standard JSF solutions are OSGi aware, but this extender helps in integrating CDI with JSF. For this a standard OSGi web
extender is needed with some additional functionality for finding Faces configuration files and tag libraries. This is functionality
provided by [PAX-WEB][4].

Documentation about the usage of this extender will be delivered soon and referenced here.

[1]: http://www.osgi.org/Specifications/HomePage
[2]: http://weld.cdi-spec.org/
[3]: http://search.maven.org/#artifactdetails|org.jboss.weld|weld-osgi-bundle|2.2.9.Final|jar
[4]: https://ops4j1.jira.com/wiki/display/paxweb/Pax+Web
