#### OSGi JEE7 CDI/JSF
JSF 2.2 makes use of CDI. The bundle here provides the bridge between the CDI extender bundles and JSF applications. It
takes care of:
- Session and request scopes via a servlet listener.
- The use (in EL) of beans defined from various CDI extended bundles.
- Using resources from multiple bundles.

The details are described in the [documentation][1].

[1]: http://www.avineas.org/uploads/jee-extender.pdf