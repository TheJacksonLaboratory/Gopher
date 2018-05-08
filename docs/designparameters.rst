Panel design
============




Panel design parameters
~~~~~~~~~~~~~~~~~~~~~~~
These are settings that determine how the panel design will be performed. Users should understand the requirements of
the probe manufacturer and choose settings that match them.

Probe length
~~~~~~~~~~~~
The ``Probe length`` should be set according to the length that will be ordered from the manufacturer. A typical value is
120 bp (this is the default in GOPHER).



Tiling Factor
~~~~~~~~~~~~~

The tiling factor specifies the aimed probe coverage within the margins. For example, if the tiling factor is 2, each
position within the margins will be covered by two probes, if possible, i.e. repetitive regions will be left. In
 GOPHER this parameter is only used to predict the expected overall number of probes for cost estimate, since GOPHER
  only preselects regions for enrichment. The actual probe design for these regions is done by the SureDesign wizard,
  which has also the parameter tiling factor or density (default 2x).


Margin size
~~~~~~~~~~~
 The ``margin size`` defines the width of the targeted regions at the outermost ends of restriction fragments that are
 earmarked to be tiled with probes. Margin size refers to the average size of the edge (margin) of the restriction
 fragments that remain after the fragmentation (sonication) step of the Capture Hi-C protocol. For instance, if a restriction
 fragment is 1000 bp long directly after restriction enzyme digestion, sonication may further fragment this segment of
 DNA into two or more smaller fragments, and the fragment that is attached to the biotin marker will be enriched.
 The baits are therefore typically designed to hybridize to the margins (and not to the center) of the restriction fragments.
 The margin size parameter should thus be set according to the expected experimental fragmentation size. We have found
 that 250 bp is a good starting point, and this value is the default in VPV.



 .. figure:: img/improve_fragment_enrichment.png
   :scale: 70 %
   :alt: Margin size


Margin strategy
~~~~~~~~~~~~~~~

For some capture Hi-C design strategies, only two probes (or baits) are placed at the outermost ends of restriction
fragments that are ought to be enriched. Enrichment and sequencing in capture Hi-C experiments effectively is concentrated
in the margins of the fragments as defined above. Typcically, fragments whose margins have a too high repeat content or
that have a too high or too low GC content are difficult to enrich and sequence and are therefore excluded from probe
designs. In many cases, however, one of the two ends of a fragment may satisfy repeat and GC criteria, while the other
end does not. VPV allows users to choose whether both ends of a fragment must satisfy these criteria (``require both margins``),
or whether all fragments are chosen for which at least one of the ends satisfies the criteria (``allow single margin``).
The default is ``allow single margin``.



 .. figure:: img/VPVmarginParams.png
   :scale: 100 %
   :alt: GOPHER margin parameters

	 GOPHER: choose margin strategy.
