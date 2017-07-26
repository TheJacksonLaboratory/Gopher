package vpvgui.gui.viewpointpanel;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.junit.Ignore;
import org.testfx.framework.junit.ApplicationTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import vpvgui.gui.analysisPane.VPRow;
import vpvgui.model.Model;
import vpvgui.model.project.ViewPoint;

import java.io.File;

public class ViewPointPresenterTest extends ApplicationTest {

    /**
     * Tested instance.
     */
    private ViewPointPresenter presenter;

    private Model model;


    @BeforeMethod
    public void setUp() throws Exception {
        presenter = new ViewPointPresenter();

        FXMLLoader loader  = new FXMLLoader(getClass().getResource("/fxml/viewpoint.fxml"));
        loader.setControllerFactory(param -> presenter);
        Parent p = loader.load();
        model = new Model();
        presenter.setModel(model);
        presenter.setViewPoint(getViewPoint());

    }

    @Test
    public void testCloseButtonAction() throws Exception {
//        TODO
    }

    @Test
    public void testRefreshUCSCButtonAction() throws Exception {
//        TODO
    }

    @Test
    public void testSaveButtonAction() throws Exception {
//        TODO
    }

    /**
     * Test that the controller was properly initialized. E.g. that it contains model, ViewPoint, etc.
     * @throws Exception
     */
    @Test
    public void testInitialization() throws Exception {

    }

    //    TODO
    @Test
    @Ignore("Don't know how to test it at the moment")
    public void testSetURL() throws Exception {
    }

    @Test
    public void testGetPane() throws Exception {
//
    }

    @Test
    public void testSendToUCSC() throws Exception {
    }

    /**
     * Create {@link ViewPoint} instance similarly as in {@link vpvgui.model.project.ViewPointTest}.
     * @return new {@link ViewPoint} object
     * @throws Exception
     */
    private static ViewPoint getViewPoint() throws Exception {
        String testReferenceSequenceID = "chr4_ctg9_hap1";
        int testGenomicPos = 10000;
        int testMaxUpstreamGenomicPos = 100; // replace initial radius with two separate max values for up and
        // downstream. This is more flexible.
        int testMaxDownstreamPos = 100;
        int testStartPos = testGenomicPos - testMaxUpstreamGenomicPos;
        int testEndPos = testGenomicPos + testMaxDownstreamPos;
        String testDerivationApproach = "INITIAL";
        String[] testCuttingPatterns = new String[]{"TCCG", "CA", "AAA"};
        IndexedFastaSequenceFile fai = new IndexedFastaSequenceFile(
                new File(ViewPointPresenterTest.class.getResource("/smallgenome/chr4_ctg9_hap1.fa").toURI()));
        return new ViewPoint(testReferenceSequenceID, testGenomicPos, testMaxUpstreamGenomicPos,
                testMaxDownstreamPos, testCuttingPatterns, fai);
    }

    @Override
    public void start(Stage stage) throws Exception {

    }
}