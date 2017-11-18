package vpvgui.model;


import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

/**
 * The purpose of this class is to provide the user with a summary that can be downloaded or copied and that will contain
 * a summary of all of the analysis parameters and results.
 * @author Peter Robinson
 * @version 0.1.1 (2017-11-17)
 */
public class VPVReport {
    private static final Logger logger = Logger.getLogger(VPVReport.class.getName());
    private Model model=null;

    private static NumberFormat dformater= NumberFormat.getInstance(Locale.US);

    public VPVReport(Model model) {
        this.model=model;
    }


    public String getReport() {
        return String.format("%s\n\n%s\n\n%s",getSettingsReport(),getDesignReport(),getRegulatoryExomeReport());


    }

    private String getDesignReport() {
        Design design = new Design(this.model);
        design.calculateDesignParameters();
        int nviewpoints=design.getN_viewpoints();
        int ngenes=design.getN_genes();
        long total_active_frags=design.getN_unique_fragments();
        double avg_n_frag=design.getAvgFragmentsPerVP();
        double avg_size=design.getAvgVPsize();
        double avg_score=design.getAvgVPscore();
        int n_totalNucleotidesInProbes=design.getN_nucleotides_in_probes();
        int totalEffectiveNucleotides=design.totalEffectiveSize();
        int tilingFactor=model.getTilingFactor();

        StringBuilder sb = new StringBuilder("Panel design results\n");

        sb.append(String.format("Number of genes: %d\n",ngenes));
        sb.append(String.format("Average number of active fragments per viewpoint: %.1f\n",avg_n_frag));
        sb.append(String.format("Total margin size: %s nucleotides\n", dformater.format(n_totalNucleotidesInProbes)));
        sb.append(String.format("Number of viewpoints: %d\n",nviewpoints));
        sb.append(String.format("Average viewpoint score: %.2f%%\n",100*avg_score));
        sb.append(String.format("Tiling factor: %dx\n",tilingFactor));
        sb.append(String.format("Number of unique fragments: %d\n",total_active_frags));
        sb.append(String.format("Average viewpoint size: %.1f nucleotides\n",avg_size));
        sb.append(String.format("Total effective size: %s kb\n", dformater.format(totalEffectiveNucleotides/100)));
        if (model==null) {
            logger.error("Model is null");
        } else if (model.getApproach()==null) {
            logger.error("Approach is null");
        }
        sb.append(String.format("Approach: %s",model.getApproach().toString()));
        return sb.toString();
    }


    private String getSettingsReport() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        StringBuilder sb = new StringBuilder("Viewpoint Viewer: Panel generation report\n");
        sb.append(dateFormat.format(date) + "\n");

        sb.append(String.format("genome_build: %s\n", model.getGenomeBuild()));
        sb.append(String.format("path_to_downloaded_genome_directory: %s\n",model.getGenomeDirectoryPath()));
        sb.append(String.format("genome_unpacked: %s\n",model.isGenomeUnpacked()));
        sb.append(String.format("genome_indexed: %s\n",model.isGenomeIndexed()));
        sb.append(String.format("refgene_path: %s\n",model.getRefGenePath()));
        sb.append(String.format("target_genes_path: %s\n",model.getTargetGenesPath()));
        sb.append(String.format("upstream size: %d\n",model.getSizeUp()));
        sb.append(String.format("downstream size: %d\n",model.getSizeDown()));
        sb.append(String.format("minimum fragment size: %d\n",model.getMinFragSize()));
        sb.append(String.format("maximum repeat content: %s%%\n",model.getMaxRepeatContentPercent()));
        sb.append(String.format("maximum GC content: %s%%\n",model.getMaxGCContentPercent()));
        sb.append(String.format("minimum GC content: %s%%\n",model.getMinGCContentPercent()));

        return sb.toString();
    }


    private String getRegulatoryExomeReport() {
        StringBuilder sb = new StringBuilder();
        Properties regprop = this.model.getRegulatoryExomeProperties();
        if (regprop==null) {
            return "";
        }
        for (String key : regprop.stringPropertyNames()) {
            sb.append(String.format("%s: %s\n",key,regprop.getProperty(key) ));
        }
        return sb.toString();
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
