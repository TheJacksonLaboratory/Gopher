package vpvgui.model;


/** The purpose of this class is to follow the steps of the initialization of the data in the GUI and the Model
 * in order to ensure that all data are filled in and to provide useful feedback to the user. The basic concept is
 * that there is an order of steps needed to be taken in the GUI, whereby some steps are dependent on previous steps,
 * and some steps are not. The dependencies are represented as Lists of items that need to be completed prior to
 * starting a given step.
 */
public class Initializer {

    private Model model=null;

    private String reason=null;


    public Initializer(Model model) {
        this.model=model;
        initializeDependencyChain();
    }

    public String getReason() {return reason; }

    private void initializeDependencyChain() {

    }

    /* No depencies to set the genome build. */
    public boolean readyForGenomeBuild() { return true; }
    /** To download the genome, the genome build has to be set. */
    public boolean readyForGenomeDownload() {
        if (model.getGenomeBuild()!=null)
            return true;
        else {
            reason="Genome build must be chosen prior to genome download!";
            return false;
        }
    }
}
