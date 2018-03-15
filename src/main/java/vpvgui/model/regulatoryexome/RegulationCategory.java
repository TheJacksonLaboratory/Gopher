package vpvgui.model.regulatoryexome;

/** These are the categories of regulatory elements in the Ensembl GTF.
 * <ul>
 *     <li>ENHANCER</li>
 *     <li>PROMOTER</li>
 *     <li>PROMOTER_FLANKING_REGION</li>
 *     <li>TF_BINDING_SITE</li>
 *     <li>CTCF_BINDING_SITE</li>
 *     <li>OPEN_CHROMATIN</li>
 * </ul>
 * In addition, we use the category "EXON" to refer to the coding exons of the target genes and {@code VIEWPOINT}
 * to refer to the segments of the viewpoints (active and inactive) of the target genes.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public enum RegulationCategory {
    PROMOTER("Promoters"),
    PROMOTER_FLANKING_REGION("Promoter-flanking-regions"),
    ENHANCER("Enhancers"),
    OPEN_CHROMATIN("Open-chromatins"),
    CTCF_BINDING_SITE("CTCF-binding-sites"),
    TF_BINDING_SITE("Transcription-factor-binding-sites"),
    EXON("Exons-of-target-genes"),
    VIEWPOINT("Viewpoints-of-target-genes");

    private final String name;

    RegulationCategory(String nomen) {
        name=nomen;
    }

    @Override
    public String toString() {return name;}



}

