package gopher.model;


import com.google.common.collect.ImmutableList;
import org.apache.log4j.Logger;

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
import java.util.stream.Collectors;

/**
 * The purpose of this class is to provide the user with a summary that can be downloaded or copied and that will contain
 * a summary of all of the analysis parameters and results.
 * @author Peter Robinson
 * @version 0.1.2 (2018-06-07)
 */
public class GopherReport {
    private static final Logger logger = Logger.getLogger(GopherReport.class.getName());
    private final Model model;

    private static final NumberFormat dformater= NumberFormat.getInstance(Locale.US);

    public GopherReport(Model model) {
        this.model=model;
    }


    private String getReport() {
        //return String.format("%s\n\n%s\n\n%s",getSettingsReport(),getDesignReport(),getRegulatoryExomeReport());
        ImmutableList.Builder<String> builder=new ImmutableList.Builder<>();
        builder.addAll(getSettingsReport());
        builder.add("\n");
        builder.addAll(getDesignReport());
        // TODO -- re add regulatory exome
        return builder.build().stream().collect(Collectors.joining(" "));
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
        Design design = new Design(this.model);
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
        builder.add(String.format("Restriction enzymes: %s\n", model.getAllSelectedEnzymeString()));
        builder.add(String.format("Number of unique fragments: %d\n",total_active_frags));
        builder.add(String.format("Average viewpoint size: %.1f nucleotides\n",avg_size));

        if (model==null) {
            logger.error("Model is null");
        } else if (model.getApproach()==null) {
            logger.error("Approach is null");
        }
        return builder.build();
    }




    private List<String> getSettingsReport() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
        builder.add("GOPHER: Panel generation report\n");
        builder.add(dateFormat.format(date) +"\n");
        builder.add(String.format("approach: %s\n", model.getApproach()));
        builder.add(String.format("genome_build: %s\n", model.getGenomeBuild()));
        builder.add(String.format("path_to_downloaded_genome_directory: %s\n",model.getGenomeDirectoryPath()));
        builder.add(String.format("genome_unpacked: %s\n",model.isGenomeUnpacked()));
        builder.add(String.format("genome_indexed: %s\n",model.isGenomeIndexed()));
        builder.add(String.format("refgene_path: %s\n",model.getRefGenePath()));
        builder.add(String.format("transcripts-name: %s", model.getTranscriptsBasename()));
        builder.add(String.format("target_genes_path: %s\n",model.getTargetGenesPath()));
        builder.add(String.format("Alignability map: %s\n",model.getAlignabilityMapPathIncludingFileNameGz()));
        builder.add(String.format("upstream size: %d\n",model.getSizeUp()));
        builder.add(String.format("downstream size: %d\n",model.getSizeDown()));
        builder.add(String.format("minimum digest size: %d\n",model.getMinFragSize()));
        builder.add(String.format("maximum GC content: %s%%\n",model.getMaxGCContentPercent()));
        builder.add(String.format("minimum GC content: %s%%\n",model.getMinGCContentPercent()));
        builder.add(String.format("Max. k-mer alignability: %d\n", model.getMaxMeanKmerAlignability()));
        if (model.getApproach().equals(Model.Approach.SIMPLE)) {
            builder.add(String.format("allow patched?: %s\n", model.getAllowPatching() ? "yes" : "no"));
        }
        builder.add(String.format("allow unbalanced margins?: %s\n", model.getAllowUnbalancedMargins() ? "yes" : "no"));
        builder.add(String.format("Minimum probe count: %d\n", model.getMinBaitCount() ));
        return builder.build();
    }


    private List<String> getRegulatoryExomeReport() {
        Properties regprop = this.model.getRegulatoryExomeProperties();
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
