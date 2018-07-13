Installing GOPHER
=================

Requirements
~~~~~~~~~~~~
GOPHER needs Java 8 or newer to run. You can determine what version of Java you have on your computer by entering the following command. ::

  $ java -version
    java version "1.8.0_144"
    Java(TM) SE Runtime Environment (build 1.8.0_144-b01)
    Java HotSpot(TM) 64-Bit Server VM (build 25.144-b01, mixed mode)

Using the prebuilt GOPHER app
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Most users should install the prebuilt App called `GOPHER.jar` that is available on the Releases section of
the GOPHER GitHub repository: https://github.com/TheJacksonLaboratory/GOPHER/releases/.



It is also possible to build the VPV application from source.
On most systems you should be able to start VPV by double clicking on the
`GOPHER.jar`. Alternatively, you can start `GOPHER.jar` from the shell with the following command. ::

  $ java -jar GOPHER.jar


**Note:** if you receive an `OutOfMemoryError` allow more memory to be allocated for Java heap using `-Xmx` option (e.g. `java -Xmx6g -jar GOPHER.jar`)


Installing and running the prebuilt GOPHER app
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The source code for GOPHER is available from the GOPHER GitHub page. ::

  https://github.com/TheJacksonLaboratory/Gopher

You can clone or download the source as with any project hosted at GitHub.
GOPHER used the maven build system. To build the App simply enter the following command. ::

  $ mvn package

This will create the  GOPHER.jar app in the "target" subdirectory.
