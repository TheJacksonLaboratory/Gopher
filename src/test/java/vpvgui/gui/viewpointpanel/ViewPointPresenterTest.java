package vpvgui.gui.viewpointpanel;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import vpvgui.model.Model;
import vpvgui.model.project.ViewPoint;

import java.io.File;

public class ViewPointPresenterTest extends ApplicationTest {

    /**
     * Tested instance.
     */
    private ViewPointPresenter presenter;

    private Model model;

    /**
     * This method initializes one {@link ViewPoint} object with real-life data.
     *
     * @return
     * @throws Exception
     */
    private static ViewPoint getViewPoint4Display() throws Exception {
        String testReferenceSequenceID = "chr11";
        int testGenomicPos = 10000;
        int testMaxUpstreamGenomicPos = 100;
        int testMaxDownstreamPos = 100;
        String[] testCuttingPatterns = new String[]{"TCCG", "CA", "AAA"};
        IndexedFastaSequenceFile fai = new IndexedFastaSequenceFile(
                new File(ViewPointPresenterTest.class.getResource("/smallgenome/chr11:0-600000.fa").toURI()));
        ViewPoint vp = new ViewPoint(testReferenceSequenceID, testGenomicPos, testMaxUpstreamGenomicPos,
                testMaxDownstreamPos, testCuttingPatterns, fai);
        vp.setTargetName("HRAS");
        // TODO - return real-life ViewPoint from here. This one has no Segments at the moment.
        return vp;
    }

    /**
     * Test that the controller was properly initialized. E.g. that it contains model, ViewPoint, etc.
     *
     * @throws Exception
     */
    @Test
    public void testInitialization() throws Exception {
        Thread.sleep(4000); // just show the screen for now.
    }


    @Override
    public void start(Stage stage) throws Exception {
        presenter = new ViewPointPresenter();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/viewpoint.fxml"));
        loader.setControllerFactory(param -> presenter);

        stage.setScene(new Scene(loader.load()));
        model = new Model();
        presenter.setModel(model);
        presenter.setViewPoint(getViewPoint4Display());
        stage.show();
    }
}