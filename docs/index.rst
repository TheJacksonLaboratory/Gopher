.. VPV documentation master file, created by
   sphinx-quickstart on Sun Sep 24 12:02:05 2017.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

Welcome to VPV's documentation!
===============================
.. toctree::
   :maxdepth: 2
   :caption: Contents:

ViewPointViewer (VPV)
~~~~~~~~~~~~~~~~~~~~~~~~~

VPV is a Java application designed to help design capture probes
for capture Hi-C and related protocols. Capture Hi-C (CHC) is based
on the Hi-C protocol but uses capture baits (similar to whole-exome sequencing) to enrich a set of viewpoints. Commonly, the viewpoints represent proximal promoter regions (surrounding the transcription start site [TSS]) of genes of interest or of all protein-coding genes.

- CHC detects interactions between viewpoint regions and distal enhancers (or other genomic regions).
- CHC has been most commonly performed with the 4-cutter DpnII or with the 6-cutter HindIII.

Quick start
~~~~~~~~~~~~~~~~~~~~~~~~~
VPV requires Java 8 or higher to run. The source code of VPV can be downloaded
from the VPV GitHub repository and the application canbe built using maven (see the GitHub page for instructions). Most users will want to download the pre-built VPV application ??WHERE??.

Currently, VPV supports design of probes for human (GRCh37, GRCh38), mouse (mm9, mm10), rat, and fly. The App guides users through the process of downloading genome and transcript data from the UCSC Genome Browser; VPV uses this data to determine the locations of restriction sites and TSS for the chosen genes. Following this, the user can upload a list of gene symbols and choose a restriction enzyme. The App will then generate a list of suggested viewpoints according to a simple or extended approach. The user can visualize the viewpoints and selected restriction fragments within their genomic context directly in the VPV app, and can select or deselect fragments. Finally, the user can export a BED file with the chosen fragments that can be used to design probes (often in conjunction with a Wizard of a manufacturer). Additionally, a file with URLs for the UCSC browser is provided so that the user can inspect the data in the UCSC Genome Browser before proceding with the experiment.

This site provides detailed explanations and tips for the various steps of this procedure.
 





Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`
