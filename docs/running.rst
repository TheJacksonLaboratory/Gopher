Running GOPHER
==============

Assuming you have built or downloaded the `GOPHER.jar` file as described in :doc:`installation`, it is simple to start the program. Enter  ::

  $ java -jar GOPHER.jar

  
or double-click on the file. You will see the following dialog.


 .. figure:: img/VPVsplash.png
   :scale: 60 %
   :alt: splash screen

 This dialog allows users to open a previously created project or to start a new one.

 If you click on the ``New project`` button, you can enter the desired name of the project and begin work. Note that GOPHER
  stores projects files in a hidden directory in the home directory: ``.gopher`` .





Creating Viewpoints
~~~~~~~~~~~~~~~~~~~

Clicking on ``Create Viewpoints`` will open a dialog in which the user can review the parameters for the current project and click on cancel to change parameters or Continue to go ahead with the analysis. Once the analysis is started,
a progress dialog will be shown.



 .. figure:: img/VPVpanel.png
   :scale: 100 %
   :alt: VPV panel

 GOPHER Panel design window.


 The panel design window allows users to review all of the viewpoints. By default, GOPHER shows viewpoints for which no fragments could be found to allow the user to
    manually review them and add Fragments by hand if desired. A score is calculated for each viewpoint as described in the main manuscript.





ViewPoint Windows
~~~~~~~~~~~~~~~~~


 Each viewpoint can be inspected in a separate window and fragments can be added or removed as desired. GOPHER fetches information about the genomic location of the viewpoint from tjhe UCSC Genome browser and displays this
    in a window. Selected fragments are highlighted with different colors.

 * Simple approach

 For the simple approach, viewpoints will have a single fragment if the fragment overlapping the TSS passes the selection criteria explained above.




 .. figure:: img/VPVsimple.png
   :scale: 100 %
   :alt: Simple RALGDS

	 Viewpoint for promoter 2 of the RALGDS gene.


For the extended approach, promoters can have multiple fragments. In the following example, GOPHER has selected 7 fragments for the 5th (most 3') of five promoters of hte ZBTB16 gene. Several of the candidate
fragments were rejected because they were too small (89 nt, 77 nt, 20 nt, and 0 nt). One 715 nt fragment was rejected because it GC content was above threshold (71.33%), and one 320 nt fragment was rejected because
of a repeat content of 73.75%. None of the reject fragments would be expected to perform well in a capture Hi-C experiment. 


 .. figure:: img/VPVextended.png
   :scale: 100 %
   :alt: Extended ZBTB16

	 GOPHER viewpoint for promoter 5 of the ZBTB16 gene.


Zooming in and out
~~~~~~~~~~~~~~~~~~
It is possible to zoom in or out, which wiull expand or contract the region of the genome that is shown. Zooming
can affect the number of fragments chosen --zooming out will cause any new fragments in the new win dow that
satisty the filter criteria to be chosen. On the other hand, zooming in will cause fragments that are no longer visible to be
deselected.


Export BED file
~~~~~~~~~~~~~~~

Current "wizards" for enrichment probe design expect a BED file as input. Users of GOPHER can export a BED file from the File menu (``File|Export BED File`` ).



	     
