#### OSGi JEE7 CDI extender
CDI does not (yet) have an OSGi enterprise standard. The bundle here however allows the usage of CDI beans in bundles:
the acts as an extender that have an "osgi.extender" capability requirement matching the filter "osgi.cdi". See more
details in the related [documentation][1].

The bundle allows beans to be exported as OSGi services and allows injection of OSGi services via annotations defined in
the `osgi.cdi.annotation` package. See documentation for details.

JSF 2.2 makes use of CDI. Therefore the bundle provides the bridge between the CDI extender bundles and 
JSF applications. It takes care of:
- Session and request scopes via a servlet listener.
- The use (in EL) of beans defined from various CDI extended bundles.
- Using resources from multiple bundles.
Note that this part is not required: it is possible to use CDI without JSF (faces imports are optional).
The details for using JSF with this CDI extender are described in the [documentation][1].

[1]: http://www.avineas.org/uploads/jee-extender.pdf