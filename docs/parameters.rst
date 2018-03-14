Parameters and Settings for VPV
===============================

Parameters that affect the overall size of the viewpoints and the thresholds for GC and repeat content are set in the main `Setup` tab.


Choosing parameters
~~~~~~~~~~~~~~~~~~~

The setup dialog allows users to set or change parameters prior to the calculation of viewpoints.


 .. figure:: img/VPVparams.png
   :scale: 100 %
   :alt: VPV params

VPV Setup window.

Users need to specificy the genome build and the restriction enzyme and upload a list of target genes, and then provide the desired parameters for viewpoints.

* Approach
  
VPV supports two approaches to capture Hi-C probe design. The ``Simple`` approach identifies one probe per target promoter, and selects one fragment that contains the transcription start site (TSS) of each promoter. Fragments are only selected if they fulfil the criteria of minimum fragment size, GC content restrictions, and maximum repeat content (see below).

The ``Extended`` approach identifies multiple fragments per promoter: all fragments located with the ``Upstream size`` (i.e., 5' to the TSS) or ``Downstream size`` (i.e., 3' to the TSS) constraints.

* Genome build

  VPV currently supports hg19 (i.e., GRCh37), GRCh38, mm9, mm10, and TODO. Choose the appropriate genome build from the pulldown menu.

Click on the ``Download`` button and choose a directory for the genome. VPV will download the corresponding genome file from the UCSC Genome Browser unless it finds the correspding files in the directory, in which case it will merely show the path to the directory. Note that the full download may take many minutes or longer depending on your network bandwidth.

The UCSC Genome files are provided as compressed (gzip) tar archives. Clicking on the start button for ``Decompress genome`` will decompress the files if necessary.
If VPV finds the unpacked files in the genome directory, it will show the message "extraction previously completed" and do nothing.

Finally, the genome FASTA files (one for each chromosome) need to be indexed. VPV indexes the files to produce ``.fai`` index files that are equivalent to those produced by samtools. If desired you can use samtools your self. If VPV finds the .fai files in the directory, it will show the message "FASTA files successfully indexed" and do nothing.

* Transcripts

  VPV requires a transcript definition file. It will automatically download the correct file from UCSC if the user clicks on the ``Transcripts`` Download button.  It is recommended to store the file in the same directory as the genome file. If VPV finds the file in the direcotry (refGene.txt.gz) it will show the path to the file and do nothing.

 * Restriction enzymes

   Clicking on the ``Restriction enzyme(s)`` Choose button will open a dialog where the user can choose a restriction enzyme. Obviously, you need to choose the enzyme that will be used in the captuyre Hi-C experiment.

   

 .. figure:: img/VPVenzymes.png
   :scale: 60 %
   :alt: VPV restriction enzymes

 This dialog allows users to choose the restriction enzyme that will be used in the capture Hi-C experiment.

* Target genes

  Clicking on the ``Enter gene list`` button will open a dialog to enter a gene list. Currently, VPV expects a list of valid (HGNC) gene symbols. Use the ``Upload`` , ``Validate`` , and ``Accept`` buttons to upload a list of gene symbols, validate them (check if the symbols are found in the RefSeq transcript file), and accept them (to go on to the next step).

   

 .. figure:: img/VPVgenes.png
   :scale: 60 %
   :alt: VPV genes

This dialog allows users to choose the genes whose promoter regions will be enriched in the capture Hi-C experiment.

* Other parameters

  There are six parameters that can be adjusted.


+-----------------------+--------------------------------------------------------------------------------+
| Item                  | Explanation                                                                    |
+=======================+================================================================================+
| Upstream size         |Number of base pairs upstream (5') of TSS                                       |
+-----------------------+--------------------------------------------------------------------------------+
| Downstream size       | Number of base pairs downstream (3') of TSS                                    |
+-----------------------+--------------------------------------------------------------------------------+
| Minimum fragment size |Size threshold for choosing a restriction fragment in base pairs                |
+-----------------------+--------------------------------------------------------------------------------+
| Max. repeat content   | Maximum percentage of repeat bases in fragment                                 |
+-----------------------+--------------------------------------------------------------------------------+
| Min. GC     content   | Minimum percentage of G and C bases in fragment                                |
+-----------------------+--------------------------------------------------------------------------------+
| Max. GC     content   | Maximum percentage of G and C bases in fragment                                |
+-----------------------+--------------------------------------------------------------------------------+



VPV will search for fragments for each transcription start site (TSS) of the indicated genes. VPV will search for fragments located within (Upstream size) base pairs 5' ot the TSS and
(Downstream size) base pairs 3' of the fragment (5' and 3' are understood with respect to the orientation of transcription of the gene). Fragments are allowed to overlap the upstream downstream boundaries.
Depending on which restriction enzyme is used the upstream and downstream size have to be set appropriately. The default values -5000 and +1500 are suitable for DpnII, but for enzymes that produce longer fragments such as HindIII larger values should be selected.
Fragments must have a certain minimum size to be efficiently enriched in capure Hi-C. 120 nucleotides is a good default value for current capture technologies. Additionally, fragments must not exceed a certain repeat content and must
have a GC content that lies within a certain range to allow accurate mapping and efficient capture and sequencing. Current design "wizards" will not allow fragments that are outside of this range. VPV will therefore choose only those
fragments that fulfil these criteria. Users may set these criteria to their maximum values to allow the design wizard to make the final decision on the fragments (then, all fragments within the indicate location will be chosen by VPV).

