Basic concepts and terminology
==============================

For Hi-C **restriction enzymes** are employed that cut the genome at specific nucleotide patterns. For instance, the enzyme
DpnII cuts at all occurrences of the pattern GATC. The resulting DNA fragments are referred to as **digests**.
The resolution of Hi-C is at the digest level, i.e. detected interactions are between digests,
which is why the target regions for capture Hi-C consists of digests.
Due to the Hi-C protocol the **baits** used for enrichment need to be placed within the **margins** of digests only.


Usable baits
~~~~~~~~~~~~

Within the framework of Gopher, a digest can only be selected, if enough usable baits can be placed within margins.
A bait is **usable** if it satisfy constraint regarding repeat and GC content.
The uniquness of any given bait is measured as its mean **alignabilty**,
and the the **GC content** is the proportion of Gs and Cs within the bait sequence.
The user can specify thresholds for alignability and GC content.

Balanced and unbalanced margins
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A **minimum number of baits** can be specified by the user.
By default, Gopher tries to place b\ :sub:`min` in each margin of a given digest.
If this is successful the digests is referred to as **balanced**.
The constraint of balanced margins can be relaxed.
If **unbalanced** margins are allowed, Gopher tries to place 2 times b\ :sub:`min` in both margins.
Alternatively, **unbalanced** digests can be added manually for individual viewpoints.

Simple viewpoints
~~~~~~~~~~~~~~~~~

**Simple viewpoints** consist of only **one digest** that most typically contains the trancription start site (TSS).
Due to the uneven distribution of restriction enzyme cutting sites across the genome, it is often the case that digests are not well centered at the TSS positions.
If the **patching** of simple viewpoints is allowed, Gopher tries to compensate for this by adding one of the adjacent digests.
The resulting viewpoints are referred to as **simple patched viewpoints** and consist of **two digests**.

Extended viewpoints
~~~~~~~~~~~~~~~~~~~

For **extended viewpoints**, a range around genomic positions (most typically a TSSs) can be specified.
Gopher tries to select all digests that overlap the specified range.
The number of digests depends on the restriction enzyme.

