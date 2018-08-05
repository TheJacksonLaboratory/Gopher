# GOPHER: Generator Of Probes for capture Hi-C Expriments at high Resolution

GOPHER is a Java App designed to help design probes for Capture Hi-C experiments. 
The VPV apps allows users to download, uncompress, and index genome FASTA files, which are used to calculate the size and repeat content of restriction fragments within candidate viewpoints. A list of candidate genes is uploaded to the App, and the app calculcates viewpoints and allows users to revise them using an interactive visualization.

The GOPHER app is currently in beta testing phase. You are welcome to download and build the app but are asked to give feedback.
Please download the app from the [Releases page](https://github.com/TheJacksonLaboratory/Gopher/releases) (currently GopherGui-0.4.7-jar-with-dependencies.jar).
You will require Java 1.8 or higher to run Gopher. On most computers you should be able to start the app with a double click. If desired, you can
also start Gopher from the command line as follows.

```aidl
$ java -jar GopherGui-0.4.0-jar-with-dependencies.jar
```

The manual for GOPHER can be found here: http://Gopher.readthedocs.io/en/latest/


### To build the read-the-docs
there seems to be a discrepancy between various systems
One of the following may work
```
$ make html
$ sphinx-autobuild . _build/html
$ sphinx-build . _build/html
```
On the mac
```
$ python3 -m pip install sphinx
$ python3 -m pip install sphinx_rtd_theme
$ sphinx-build . _build/html
```
