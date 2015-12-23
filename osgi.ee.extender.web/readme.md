#### OSGi JEE7 WEB extender
OSGi web extender that (partially) supports the functionality required by chapter 128 of the OSGi
compendium/enterprise specification. 

##### How to set-up?
This bundle uses an OSGI HTTP service (as specified in chapter 102 of the compendium specification) as 
starting point for extending web bundles. Therefore, a standard HTTP service implementation (from either the Eclipse Equinox or [Apache Felix][1] projects) is needed. After that, just deploy the bundle in the OSGi environment and declare a bundle with a Web-ContextPath bundle header to have it picked up by the extender.

[1]: http://felix.apache.org/documentation/subprojects/apache-felix-http-service.html
