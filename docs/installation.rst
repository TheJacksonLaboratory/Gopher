Installing VPV
===============================

Requirements
~~~~~~~~~~~~~~~~~~~~~~~~~
VPV needs Java 8 or newer to run. You can determine what version of Java you have on your computer by entering the following command. ::

  $ java -version
    java version "1.8.0_144"
    Java(TM) SE Runtime Environment (build 1.8.0_144-b01)
    Java HotSpot(TM) 64-Bit Server VM (build 25.144-b01, mixed mode)

Using the prebuilt VPV app
~~~~~~~~~~~~~~~~~~~~~~~~~~
Most users should install the prebuilt App called `VPViewer.jar` but it is also possible to build the VPV application from source.
On most systems you should be able to start VPV by double clicking on the
`VPViewer.jar`. Alternatively, you can start `VPViewer.jar` from the shell with the following command. ::

  $ java -jar VPViewer.jar


The prebuilt VPV file will be available at the VPV GitHUb page following publication.





Installing and running the prebuilt ViewPointViewer (VPV) app
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The source code for VPViewer is available from the VPV GitHub page. :: 

  https://github.com/TheJacksonLaboratory/VPV

You can clone or download the source as with any project hosted at GitHub.
VPViewer used the maven build system. To build the App simply enter the following command. ::

  $ mvn package

This will create the  VPViewer.jar app in the "target" subdirectory.
