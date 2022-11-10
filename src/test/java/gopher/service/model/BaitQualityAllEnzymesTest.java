package gopher.service.model;

import gopher.gui.factories.PopupFactory;
import gopher.io.RefGeneParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BaitQualityAllEnzymesTest {

    private static BaitQualityAllEnzymes bqae;


    @BeforeAll
    public static void init() {
        String refgenePath = "/home/robinp/data/gopher/refGene.txt.gz";
        var bqae = new BaitQualityAllEnzymes(refgenePath);
        bqae.run();
    }

    @Test
    public void testCtor() {
        assertTrue(true);
    }





}
