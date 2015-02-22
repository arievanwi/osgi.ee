#### SLF4J bridge for usage in OSGi
SLF4J contains a standard [bridge][1] for briding Java Util Logging (JUL) to SLF4J. This bundle just sets this bridge to make sure
that all logs done via JUL are automatically redirected to SLF4J when this bundle is started. Nothing fancy.

[1]: https://github.com/qos-ch/slf4j/blob/master/jul-to-slf4j/src/main/java/org/slf4j/bridge/SLF4JBridgeHandler.java