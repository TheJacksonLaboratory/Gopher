# VPV
Viewpoint Viewer
A Java App designed to help design probes for Capture Hi-C. 
The VPV apps allows users to download, uncompress, and index genome FASTA files, which are used to calculate the size and repeat content of restriction fragments within candidate viewpoints. A list of candidate genes is uploaded to the App, and the app calculcates viewpoints and allows users to revise them using an interactive visualization.


## To build the read-the-docs
there seems to be a discrepancy between various systems
One of the following may work
```
$ make html
$ sphinx-autobuild . _build/html
$ sphinx-build . _build/html
```
