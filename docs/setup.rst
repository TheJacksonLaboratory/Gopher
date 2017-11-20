VPV: Simple and Extended Approach
=================================

VSV offers two approaches for choosing viewpoints and probes.

To choose one of the two viewpoint design approaches, select Simple or Extended from the pulldown menu at the top of the setup pane.


Simple
~~~~~~~~~~~~~~~~~~~~~~~~~
In the simple approach, VPV tries to select the fragment that
overlaps the transcriptional start site (TSS) and additionally
satisfies the following requirements.

* GC content of the fragment is within the minimum and maximum limits defined on the setup tab
* The repeat content of the margins of a fragment is not higher than the defined limit (see below for a definition of fragment margin)
* The length of the fragment is not less than the lower boundary defined in the setup tab, and not more than 20,000 nucleotides


If the restriction fragment that overlaps the TSS does not satisfy all of these criteria, then no restriction fragment is selected.



Extended
~~~~~~~~~~~~~~~~~~~~~~~~~
In the extended approach, VPV tries to select multiple restrictions fragments that surround the TSS of a candidate transcript in order to provide a high-resolution view of contacts the the promoter associated with the TSS. VPV will choose every fragment within the boundaries defined by Upstream size and Downstream size in the setup tab. For instance, if upstream size is set to 5000 and downstream size is set to 2000, then VPV will select all restriction fragments that are at least partially located within the 5000 nucleotides 5' (upstream) of the TSS and within 2000 nucleotides 3' (downstream) of the TSS. Note that if a fragment overlaps the upstream or downstream limit by at least one nucleotide it will be chosen if the criteria are fulfiled. The criteria for GC and repeat content are identical to those of the simple approach.


Margin size
~~~~~~~~~~~
Traditionally, only two probes (or baits) are placed at the outermost ends of restriction fragments that are ought to be enriched. This is reasonable, if many fragments are targeted, e.g. all fragments that contain TSSs. However, if only the TSS of a few hundred up to thousand genes are targeted, the number of probes is not a limiting factor, and the ends of restriction fragments can be tiled with multiple probes in order to improve the enrichment. The margin size defines the width of the targeted regions at the outermost ends of restriction fragments that are earmarked to be tiled with probes.

 .. figure:: img/improve_fragment_enrichment.png
   :scale: 70 %
   :alt: Margin size


Tiling Factor
~~~~~~~~~~~~~

The tiling factor specifies the aimed probe coverage within the margins. For example, if the tiling factor is 2, each position within the margins will be covered by two probes, if possible, i.e. repetitive regions will be left. In VPV this parameter is only used to predict the expected overall number of probes for cost estimate, since VPV only preselects regions for enrichment. The actual probe design for these regions is done by the SureDesign wizard, which has also the parameter tiling factor or density (default 2x).
