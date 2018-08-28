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

Please consult the `Java download page <https://java.com/en/>_` for more information.

~~~~~~~~~~~~~
Memory errors
~~~~~~~~~~~~~
If you receive an ``OutOfMemoryError`` allow more memory to be allocated for Java heap using ``-Xmx`` option. ::

    java -Xmx6g -jar GOPHER.jar

