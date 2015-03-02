#### OSGi JEE7 CDI extender
CDI does not (yet) have a OSGi enterprise standard. The bundle here however allows the usage of CDI beans in bundles:
the acts as an extender that have an "osgi.extender" capability requirement matching the filter "osgi.cdi". See more
details in the related [documentation][1].

The bundle allows beans to be exported as OSGi services and allows injection of OSGi services via annotations defined in
the `osgi.cdi.annotation` package. See documentation for details.

[1]: http://www.avineas.org/uploads/jee-extender.pdf