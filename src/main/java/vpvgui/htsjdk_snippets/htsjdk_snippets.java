package main.java.vpvgui.htsjdk_snippets;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;

import java.io.File;
import java.io.FileNotFoundException;

public class htsjdk_snippets {

    // learn how to use junit testing
    // ------------------------------

    private static boolean happy = false;
    private String name;

    public htsjdk_snippets(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static boolean isHappy() {
        return happy;
    }

    public static void playWithRock() {
        happy = true;
    }

    // learn how to use htsjdk API
    // ---------------------------

    // open FASTA file
    public static final IndexedFastaSequenceFile openFastaFile(String path) {
        final File fasta = new File(path); // index has to be there
        IndexedFastaSequenceFile fastaReader = null;
        try {
            fastaReader = new IndexedFastaSequenceFile(fasta);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return fastaReader;
    }

    // find pattern in given FASTA file
    public static void FindPattern() {

    }

}

