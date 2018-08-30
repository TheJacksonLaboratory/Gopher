===============
Troubleshooting
===============

~~~~~~~~~~~~~~~~~
Connection issues
~~~~~~~~~~~~~~~~~

Many Gopher functions depend on an active internet connection including (obviously) the downloads.
The visualization of viewpoints is implemented with a call to the UCSC genome browser. Therefore,
you must be onlne for Gopher to work correctly, and if you receive an error message or if a blank
page is shown for the visualization of viewpoints, you should check your internet connection.

~~~~~
Proxy
~~~~~
If you are behind a proxy server, then you will need to enter the HTTP proxy and port with
 the Edit|Set proxy menu item.

~~~~~~~~~~~~
Java version
~~~~~~~~~~~~
GOPHER requires a Java runtime environment (JRE) of 1.8 or higher to run. If one attempts to start Java with an
older JRE, an error message such as the following may appear: ::

    Unsupported major.minor version 52.0

Please consult the `Java download page <https://java.com/en/>_` for more information. To determine the Java version
on your computer, open a terminal window and enter ::

    $ java -version

You should then seen a response such as ::

    java version "1.8.0_181"
    Java(TM) SE Runtime Environment (build 1.8.0_181-b13)
    Java HotSpot(TM) 64-Bit Server VM (build 25.181-b13, mixed mode)

The version in this example is thus 1.8.0_181.

~~~~~~~~~~~~~
Memory errors
~~~~~~~~~~~~~
We recommend starting GOPHER with at least 2 GB memory. The amount of memory required by GOPHER will depend on the
size of the panel and GOPHER may require more memory with very large panels. GOPHER will display an error dialog
stating ``Out of Memory Error`` if it runs out of memory. In this case, you will need to close the program,
adjust the memry settings, and restart GOPHER.

There are several ways to adjust the amount of memory that the Java Virtual Machine provides to Java apps. When starting
an app such as GOPHER from the command line, you can determine the amount of Minimum and Maximum memory provided to the
app as follows. ::

    $ java -Xms2g -Xmx6g -jar GOPHER.jar

This command will start GOPHER with at least 2 gigabytes of RAM (-Xms2g) and allow GOPHER to use up to 6 gigabytes of
RAM (-Xmx6g).


