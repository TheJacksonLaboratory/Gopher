package vpvgui.model;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class DesignTest {

    @BeforeClass
    public static void setup() throws Exception {

        // create a model for testing
        Model testModel = new Model();

        // use model to create a design for testing
        Design testDesign = new Design(testModel);

    }

    @Test
    public void getTotalNumberOfProbeNucleotides() throws Exception {
    }

}