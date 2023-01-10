# GOPHER: Generator Of Probes for capture Hi-C Expriments at high Resolution

GOPHER is a Java App designed to help design probes for Capture Hi-C experiments. 
GOPHER allows users to download, uncompress, and index genome FASTA files, which are 
used to calculate the size and repeat content of restriction fragments within candidate viewpoints. 
A list of candidate genes is uploaded to the App, and the app calculcates viewpoints and allows 
users to revise them using an interactive visualization.

Please download the app from the [Releases page](https://github.com/TheJacksonLaboratory/Gopher/releases) 
(currently GOPHER-0.8.18.jar).
You will require Java 17 or higher to run Gopher.  On most computers you should be able to start the app with 
a double click. (Note to M1/M2 MacIntosh users - we have noted that problems occur with JavaFX applications such as GOPHER
on M1 Macs with Java 17; we recommend using the Java 19 JDK from [Azul](https://www.azul.com/) Zulu). If desired, you can
also start Gopher from the command line as follows.

```aidl
$ java -jar GOPHER-0.8.18.jar
```

We recommend starting GOPHER with 2Gb (min)/6Gb (max) memory.

```aidl
$ java -Xms2g -Xmx6g -jar GOPHER-0.5.8.jar
```

GOPHER will run without problems on 2GB for small panels (-Xmx2g), but 6GB is preferable for probe design
for all protein-coding genes or other very larger panels.

The manual for GOPHER can be found here: http://Gopher.readthedocs.io/en/latest/

See [Hansen P, et al., 2019, GOPHER: Generator Of Probes for capture Hi-C Experiments at high Resolution. 
BMC Genomics 20:40](https://pubmed.ncbi.nlm.nih.gov/30642251/) for more information
