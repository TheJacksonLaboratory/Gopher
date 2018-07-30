.. GOPHER documentation master file, created by
sphinx-quickstart on Sun Sep 24 12:02:05 2017.
You can adapt this file completely to your liking, but it should at least
contain the root `toctree` directive.

Welcome to GOPHER's documentation!
==================================
.. toctree::
   :maxdepth: 2
   :caption: Contents:

   installation
   chc_intro
   concepts
   approaches
   graphical_user_interface
   parameters
   designparameters
   running
   output
   Regulatory Exome <regulatoryexome>

GOPHER
~~~~~~

GOPHER (Generator Of Probes for capture Hi-C Experiments at high Resolution)
is a Java application that helps users design enrichment probes
for capture Hi-C and related protocols. Capture Hi-C (CHC) is based
on the Hi-C protocol but uses capture baits (similar to whole-exome sequencing) to enrich a set of viewpoints.
Commonly, the viewpoints represent proximal promoter regions (surrounding the transcription start site; TSS)
of genes of interest or of all protein-coding genes.

- CHC detects interactions between viewpoint regions and distal enhancers (or other genomic regions).
- CHC has been most commonly performed with the 4-cutter DpnII or with the 6-cutter HindIII.



Quick start
~~~~~~~~~~~
GOPHER requires Java 8 or higher to run. The source code of GOPHER can be downloaded
from the GOPHER GitHub repository (https://github.com/TheJacksonLaboratory/Gopher/), but
most users will want to download the pre-compiled application from the
Releases page at <https://github.com/TheJacksonLaboratory/Gopher/releases>. If desired, the
application can be easily built from source using maven.


Currently, GOPHER supports design of probes for human (GRCh37, GRCh38) and mouse (mm9, mm10). The App
guides users through the process of downloading genome and transcript data from the UCSC Genome Browser; GOPHER
 uses this data to determine the locations of restriction sites and TSS for the chosen genes. Following this, the
 user can upload a list of gene symbols and choose a restriction enzyme. The App will then generate a list of suggested
 viewpoints according to a simple or extended approach. The user can visualize the viewpoints and selected restriction
 fragments within their genomic context directly in the GOPHER app, and can select or deselect fragments. Finally, the user
 can export a BED file with the chosen fragments that can be used to design probes (often in conjunction with a Wizard
 of a manufacturer). Additionally, a file with URLs for the UCSC browser is provided so that the user can inspect the
 data in the UCSC Genome Browser before proceding with the experiment.

This site provides detailed explanations and tips for the various steps of this procedure.
 



