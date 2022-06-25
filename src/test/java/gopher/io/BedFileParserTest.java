package gopher.io;

import gopher.exception.GopherException;
import gopher.service.model.GopherGene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


public class BedFileParserTest {

    private static  List<GopherGene> gopherGeneList;


    @BeforeAll
    public static void setup() throws GopherException {
        ClassLoader classLoader = RefGeneParserTest.class.getClassLoader();
        String refgene = classLoader.getResource("gwas-test.bed").getFile();
        Path path = Paths.get("src","test","resources","gwas-test.bed");
        File f = path.toFile();

        BedFileParser parser = new BedFileParser(f.getAbsolutePath());
        gopherGeneList= parser.getGopherGeneList();
    }

    @Test
    public void testNumberOfGwasHitsRetrieved() {
        int expected=6;
        assertEquals(expected,gopherGeneList.size());
    }

    @Test
    public void testGetTheCorrectGeneNames() {
        // gene names are taken from the name column of the BED file
        Set<String> names = new HashSet<>();
        gopherGeneList.forEach(gg -> names.add(gg.getGeneSymbol()));
        assertTrue(names.contains("rs11574637"));
        assertTrue(names.contains("rs17083844"));
        assertTrue(names.contains("rs12141391"));
        assertTrue(names.contains("rs7574865"));
        assertTrue(names.contains("rs979233"));
        assertTrue(names.contains("rs2187668"));
        assertFalse(names.contains("madeup"));
    }

    /** The positions for "rs11574637" are 31357552 31357553 in BED format*/
    @Test
    public void testGetTheCorrectStartPosition() {
        Map<String,GopherGene> genemap = new HashMap<>();
        gopherGeneList.forEach(gg -> genemap.put(gg.getGeneSymbol(),gg));
        GopherGene gg = genemap.get("rs11574637");
        Integer expected=31357553;
        List<Integer> tss = gg.getTSSlist();
        assertEquals(1,tss.size());
        assertEquals(expected,tss.get(0));
    }

}
