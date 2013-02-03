com.google.gwt.servlet
======================

This project creates an OSGi bundle for running GWT servlets in OSGi

The goal of this project is to minimize the size of the JAR that is being produced, since the 2 library files are 8.2 MB together. Almost all the client packages are no needed at the server-side though. Therefor the current version creates an JAR file of 2.2 MB.

Warning: problems with classpaths
---------------------------------

There is an issue with GWT that I haven't really been able to solve. GWT uses the classpath to discover the classes for deserialization. This means that all bundles that are running using GWT, need to be in the same classpath, otherwise you will get ClassNotFoundExceptions. The easiest solution that I'm using now is to run these bundles with the GWT bundle as its Fragement-Host.
