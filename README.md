# GOPHER: Generator Of Probes for capture Hi-C Expriments at high Resolution

GOPHER is a Java App designed to help design probes for Capture Hi-C experiments. 
The VPV apps allows users to download, uncompress, and index genome FASTA files, which are used to calculate the size and repeat content of restriction fragments within candidate viewpoints. A list of candidate genes is uploaded to the App, and the app calculcates viewpoints and allows users to revise them using an interactive visualization.

The GOPHER app is currently in beta testing phase. You are welcome to download and build the app but are asked to give feedback.
Please download the app from the [Releases page](https://github.com/TheJacksonLaboratory/Gopher/releases) (currently GOPHER-0.5.5.jar).
You will require Java 1.8 to run Gopher. (Java 1.10 currently will not work; GOPHER will suppor the next major release of Java, 11).
On most computers you should be able to start the app with a double click. If desired, you can
also start Gopher from the command line as follows.

```aidl
$ java -jar GOPHER-0.5.5.jar
```

We recommend starting GOPHER with 2Gb (min)/4Gb (max) memory.

```aidl
$ java -Xms2g -Xmx4g -jar GOPHER-0.5.5.jar
```



The manual for GOPHER can be found here: http://Gopher.readthedocs.io/en/latest/
