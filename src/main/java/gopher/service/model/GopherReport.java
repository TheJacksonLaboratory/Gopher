package gopher.service.model;


import com.google.common.collect.ImmutableList;
import gopher.service.GopherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * The purpose of this class is to provide the user with a summary that can be downloaded or copied and that will contain
 * a summary of all of the analysis parameters and results.
 * @author Peter Robinson
 * @version 0.1.2 (2018-06-07)
 */
public class GopherReport {
    private static final Logger LOGGER = LoggerFactory.getLogger(GopherReport.class.getName());
    private final GopherService service;

    private static final NumberFormat dformater= NumberFormat.getInstance(Locale.US);

    private final Design design;

    @Autowired
    public GopherReport(GopherService service) {
        this.service = service;
        this.design = new Design(service);
    }


    private String getReport() {
        return String.join("", getSettingsReport());
    }

    public List<String> getReportList() {
        ImmutableList.Builder<String> builder=new ImmutableList.Builder<>();
        builder.addAll(getSettingsReport());
        builder.add("\n");
        builder.addAll(getDesignReport());
        return builder.build();
    }

    /**
     * @return A list of string representing the Design results.
     */
    private List<String> getDesignReport() {
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
        design.calculateDesignParameters();
        int nviewpoints=design.getN_viewpoints();
        int ngenes=design.getN_genes();
        long total_active_frags=design.getN_unique_fragments();
        double avg_n_frag=design.getAvgFragmentsPerVP();
        double avg_size=design.getAvgVPsize();
        double avg_score=design.getAvgVPscore();
        int n_totalNucleotidesInProbes=design.getN_nucleotides_in_unique_fragment_margins();

        builder.add("Panel design results\n");

        builder.add(String.format("Number of genes: %d\n",ngenes));
        builder.add(String.format("Average number of active fragments per viewpoint: %.1f\n",avg_n_frag));
        builder.add(String.format("Total margin size: %s nucleotides\n", dformater.format(n_totalNucleotidesInProbes)));
        builder.add(String.format("Number of viewpoints: %d\n",nviewpoints));
        builder.add(String.format("Average viewpoint score: %.2f%%\n",100*avg_score));
        builder.add(String.format("Restriction enzymes: %s\n", service.getAllSelectedEnzymeString()));
        builder.add(String.format("Number of unique fragments: %d\n",total_active_frags));
        builder.add(String.format("Average viewpoint size: %.1f nucleotides\n",avg_size));
        return builder.build();
    }




    private List<String> getSettingsReport() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
        builder.add("GOPHER: Panel generation report\n");
        builder.add(dateFormat.format(date) +"\n");
        builder.add(String.format("approach: %s\n", service.getApproach()));
        builder.add(String.format("genome_build: %s\n", service.getGenomeBuild()));
        builder.add(String.format("path_to_downloaded_genome_directory: %s\n",service.getGenomeDirectoryPath()));
        builder.add(String.format("genome_unpacked: %s\n",service.isGenomeUnpacked()));
        builder.add(String.format("genome_indexed: %s\n",service.isGenomeIndexed()));
        builder.add(String.format("refgene_path: %s\n",service.getRefGenePath()));
        builder.add(String.format("transcripts-name: %s\n", service.getTranscriptsBasename()));
        builder.add(String.format("target_genes_path: %s\n",service.getTargetGenesPath()));
        builder.add(String.format("Alignability map: %s\n",service.getAlignabilityMapPathIncludingFileNameGz()));
        builder.add(String.format("upstream size: %d\n",service.getSizeUp()));
        builder.add(String.format("downstream size: %d\n",service.getSizeDown()));
        builder.add(String.format("minimum digest size: %d\n",service.getMinFragSize()));
        builder.add(String.format("maximum GC content: %s%%\n",service.getMaxGCContentPercent()));
        builder.add(String.format("minimum GC content: %s%%\n",service.getMinGCContentPercent()));
        builder.add(String.format("Max. k-mer alignability: %d\n", service.getMaxMeanKmerAlignability()));
        if (service.getApproach().equals(Approach.SIMPLE)) {
            builder.add(String.format("allow patched?: %s\n", service.getAllowPatching() ? "yes" : "no"));
        }
        builder.add(String.format("allow unbalanced margins?: %s\n", service.getAllowUnbalancedMargins() ? "yes" : "no"));
        builder.add(String.format("Minimum probe count: %d\n", service.getMinBaitCount() ));
        return builder.build();
    }


    private List<String> getRegulatoryExomeReport() {
        Properties regprop = this.service.getRegulatoryExomeProperties();
        if (regprop==null) {
            return ImmutableList.of();
        }
        ImmutableList.Builder<String> builder=new ImmutableList.Builder<>();
        for (String key : regprop.stringPropertyNames()) {
            builder.add(String.format("%s: %s\n",key,regprop.getProperty(key) ));
        }
        return builder.build();
    }

    /** Output the regulatory report to a simple text file. */
    public void outputRegulatoryReport(String path) {
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(path));
            bw.write(getReport());
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
