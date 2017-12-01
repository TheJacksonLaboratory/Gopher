Parameters and Settings for VPV
===============================

There are two places for users to specify parameters and settings, the `Design parameters` menu and the main `Setup` tab.


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
Fragments must have a certain minimum size to be efficiently enriched in capure Hi-C. 120 nucleotides is a good default value for current capture technologies. Additionally, fragments must not exceed a certain repeat content and must
have a GC content that lies within a certain range to allow accurate mapping and efficient capture and sequencing. Current design "wizards" will not allow fragments that are outside of this range. VPV will therefore choose only those
fragments that fulfil these criteria. Users may set these criteria to their maximum values to allow the design wizard to make the final decision on the fragments (then, all fragments within the indicate location will be chosen by VPV).



Panel design parameters
~~~~~~~~~~~~~~~~~~~~~~~
These are settings that determine how the panel design will be performed. Users should understand the requirements of the probe manufacturer and choose settings that match them.

Probe length
~~~~~~~~~~~~
The ``Probe length`` should be set according to the length that will be ordered from the manufacturer. A typical value is 120 bp (this is the default in VPV).


Tiling factor
~~~~~~~~~~~~~
The ``Tiling factor`` refers to the average number of times that a position in the target will covered by a probe. Increasing the tiling factor is expected to increase the overall efficiency of capture for the targeted regions. Bait Tiling creates baits that evenly cover the selected genomic regions, whereby the exact sequences are determined by the probe manufacturer. We advise that it is better to use the highest tiling factor that is possible given the available number of probes.

Margin size
~~~~~~~~~~~
The ``Margin size`` refers to the average size of the edge (margin) of the restriction fragments that remain after the fragmentation (sonication) step of the Capture Hi-C protocol. For instance, if a restriction fragment is 1000 bp long directly after restriction enzyme digestion, sonication may further fragment this segment of DNA into two or more smaller fragments, and the fragment that is attached to the biotin marker will be enriched. The baits are therefore typically designed to hybridize to the margins (and not to the center) of the restriction fragments. The margin size parameter should thus be set according to the expected experimental fragmentation size. We have found that 250 bp is a good starting point, and this value is the default in VPV.


Margin strategy
~~~~~~~~~~~~~~~
Enrichment and sequencing in capture Hi-C experiments effectively is concentrated in the margins of the fragments as defined above. Typcically, fragments whose margins have a too high repeat content or that have a too high or too low GC content are difficult to enrich and sequence and are therefore excluded from probe designs. In many cases, however, one of the two ends of a fragment may satisfy repeat and GC criteria, while the other end does not. VPV allows users to choose whether both ends of a fragment must satisfy these criteria ("require both margins), or whether all fragments are chosen for which at least one of the ends satisfies the critera.



 .. figure:: img/VPVmarginParams.png
   :scale: 100 %
   :alt: VPV margin parameters

	 VPV: choose margin strategy.
