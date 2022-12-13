package gopher.service.impl;

import gopher.gui.factories.PopupFactory;
import gopher.io.Faidx;
import gopher.io.Platform;
import gopher.io.RefGeneParser;
import gopher.service.model.Approach;
import gopher.service.model.GopherGene;
import gopher.service.model.GopherModel;
import gopher.service.model.RestrictionEnzyme;
import gopher.service.model.genome.Genome;
import gopher.service.model.viewpoint.ViewPoint;
import gopher.service.GopherService;
import gopher.util.SerializationManager;
import javafx.scene.control.ProgressIndicator;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class GopherServiceImpl implements GopherService, Serializable {
    private final static Logger LOGGER = LoggerFactory.getLogger(GopherServiceImpl.class);
    @Autowired
    private GopherModel model;

    @Autowired
    public GopherServiceImpl(GopherModel model) {
        this.model = model;
    }


    @Override
    public boolean serialize() {
        String projectname = this.model.getProjectName();
        if (projectname == null) {
            PopupFactory.displayError("Error", "Could not get viewpoint name (should never happen). Will save with default");
            projectname = "default";
        }
        String serializedFilePath = Platform.getAbsoluteProjectPath(projectname);
        return serializeToLocation(serializedFilePath);
    }

    @Override
    public boolean serializeToLocation(String path) {
        if (path == null) {
            PopupFactory.displayError("Error", "Could not get file name for saving project file.");
            return false;
        }
        try {
            SerializationManager.serializeModel(this.model, path);
        } catch (IOException e) {
            PopupFactory.displayException("Error", "Unable to serialize Gopher viewpoint", e);
            return false;
        }
        LOGGER.trace("Serialization successful to file " + path);
        return true;
    }

    @Override
    public boolean isGenomeUnpacked() {
        return model.isGenomeUnpacked();
    }

    @Override
    public boolean isGenomeIndexed() {
        return model.isGenomeIndexed();
    }


    @Override
    public String getRefGenePath() {
        return model.getRefGenePath();
    }

    @Override
    public boolean alignabilityMapPathIncludingFileNameGzExists() {
        return model.alignabilityMapPathIncludingFileNameGzExists();
    }

    @Override
    public List<RestrictionEnzyme> getChosenEnzymelist() {
        if (model.getChosenEnzymelist() == null) return List.of();
        return model.getChosenEnzymelist();
    }

    @Override
    public String getAllSelectedEnzymeString() {
        return model.getAllSelectedEnzymeString();
    }

    @Override
    public boolean getAllowUnbalancedMargins() {
        return model.getAllowUnbalancedMargins();
    }

    @Override
    public boolean getAllowPatching() {
        return model.getAllowPatching();
    }

    @Override
    public void setProjectName(String projectname) {
        this.model.setProjectName(projectname);
    }

    @Override
    public void setModel(GopherModel mod) {
        this.model = mod;
    }

    @Override
    public int getSizeUp() {
        return model.getSizeUp();
    }

    @Override
    public int getSizeDown() {
        return model.getSizeDown();
    }

    @Override
    public int getProbeLength() {
        return model.getProbeLength();
    }

    @Override
    public int getMarginSize() {
        return model.getMarginSize();
    }

    @Override
    public GopherModel.TargetType getTargetType() {
        return model.getTargetType();
    }

    @Override
    public Approach getApproach() {
        return model.getApproach();
    }

    @Override
    public int getN_validGeneSymbols() {
        return model.getN_validGeneSymbols();
    }

    @Override
    public boolean useExtendedApproach() {
        return model.useExtendedApproach();
    }

    @Override
    public boolean useSimpleApproach() {
        return model.useSimpleApproach();
    }

    @Override
    public String getGenomeBuild() {
        return model.getGenomeBuild();
    }

    @Override
    public void setClean(boolean b) {
        model.setClean(b);
    }

    @Override
    public void setMarginSize(int marginsize) {
        model.setMarginSize(marginsize);
    }

    @Override
    public void setProbeLength(int baitlen) {
        model.setProbeLength(baitlen);
    }

    @Override
    public void setMinBaitCount(int minbait) {
        model.setMinBaitCount(minbait);
    }

    @Override
    public void setMaxGCcontent(double v) {
        model.setMaxGCcontent(v);
    }

    @Override
    public void setMaxMeanKmerAlignability(int kmerAlign) {
        model.setMaxMeanKmerAlignability(kmerAlign);
    }

    @Override
    public void setSizeDown(int i) {
        model.setSizeDown(i);
    }

    @Override
    public void setSizeUp(int i) {
        model.setSizeUp(i);
    }

    @Override
    public void setMinFragSize(int i) {
        model.setMinFragSize(i);
    }

    @Override
    public void setMaxRepeatContent(double v) {
        model.setMaxRepeatContent(v);
    }

    @Override
    public void setMinGCcontent(double v) {
        model.setMinGCcontent(v);
    }

    @Override
    public void setGenomeBuild(String build) {
        model.setGenomeBuild(build);
    }

    @Override
    public boolean checkDownloadComplete(String absolutePath) {
        return model.checkDownloadComplete(absolutePath);
    }

    @Override
    public void setGenomeDirectoryPath(String absolutePath) {
        LOGGER.trace("Setting genome directory path to {}", absolutePath);
        model.setGenomeDirectoryPath(absolutePath);
    }

    @Override
    public String getGenomeBasename() {
        return model.getGenomeBasename();
    }

    @Override
    public void setTranscriptsBasename(String transcriptName) {
        model.setTranscriptsBasename(transcriptName);
    }

    @Override
    public void setRefGenePath(String abspath) {
        model.setRefGenePath(abspath);
    }

    @Override
    public Genome getGenome() {
        return model.getGenome();
    }

    @Override
    public void setGenomeUnpacked() {
        model.setGenomeUnpacked();
    }

    @Override
    public void indexGenome(ProgressIndicator genomeIndexPI) {
        Faidx manager = new Faidx(this.model,genomeIndexPI);
        if (! manager.genomeFileExists()) {
            PopupFactory.displayError("Could not find genome file",
                    "Download and extract genome file before indexing step!");
            return;
        }

        manager.setOnSucceeded(event ->{
            int n_chroms = manager.getContigLengths().size();
            String message = String.format("%d chromosomes in %s successfully indexed.",
                    n_chroms,
                    model.getGenome().getGenomeFastaName());
            // indexGenomeLabel.setText(message);
            LOGGER.debug(message);
            model.setIndexedGenomeFastaIndexFile(manager.getGenomeFastaIndexPath());
            model.setGenomeIndexed();
        } );
        manager.setOnFailed(event-> {
            //  indexGenomeLabel.setText("FASTA indexing failed");
            PopupFactory.displayError("Failure to index Genome FASTA file.",
                    manager.getException().getMessage());
        });
        Thread th = new Thread(manager);
        th.setDaemon(true);
        th.start();
    }

    @Override
    public void setAlignabilityMapPathIncludingFileNameGz(String alignabilityMapPathIncludingFileNameGz) {
        model.setAlignabilityMapPathIncludingFileNameGz(alignabilityMapPathIncludingFileNameGz);
    }

    @Override
    public void setChromInfoPathIncludingFileNameGz(String chromInfoPathIncludingFileNameGz) {
        model.setChromInfoPathIncludingFileNameGz(chromInfoPathIncludingFileNameGz);
    }

    @Override
    public String getAlignabilityFtp(String genomeBuild) {
        return switch (genomeBuild) {
            case "hg19" -> "ftp://ftp.jax.org/robinp/GOPHER/alignability_maps/wgEncodeCrgMapabilityAlign50mer.hg19.bedGraph.gz";
            case "mm9" ->  "ftp://ftp.jax.org/robinp/GOPHER/alignability_maps/wgEncodeCrgMapabilityAlign50mer.mm9.bedGraph.gz";
            case "hg38" -> "ftp://ftp.jax.org/robinp/GOPHER/alignability_maps/hg38_50.m2.bedGraph.gz";
            case "mm10" ->  "ftp://ftp.jax.org/robinp/GOPHER/alignability_maps/mm10_50.m2.bedGraph.gz";
            case "xenTro9" -> "ftp://ftp.jax.org/robinp/GOPHER/alignability_maps/xenTro9_50.bedGraph.gz";
            case "danRer10" -> "ftp://ftp.jax.org/robinp/GOPHER/alignability_maps/danRer10_50.bedGraph.gz";
            default -> null; // should never happen
        };
    }

    @Override
    public String getAlignabilityHttp(String genomeBuild) {
        return switch (genomeBuild) {
            case "hg19" ->  "http://hgdownload.cse.ucsc.edu/goldenPath/hg19/database/chromInfo.txt.gz";
            case "mm9" ->  "http://hgdownload.cse.ucsc.edu/goldenPath/mm9/database/chromInfo.txt.gz";
            case "hg38" ->  "http://hgdownload.cse.ucsc.edu/goldenPath/hg38/database/chromInfo.txt.gz";
            case "mm10" ->  "http://hgdownload.cse.ucsc.edu/goldenPath/mm10/database/chromInfo.txt.gz";
            case "xenTro9" ->  "http://hgdownload.cse.ucsc.edu/goldenPath/xenTro9/database/chromInfo.txt.gz";
            case "danRer10" -> "http://hgdownload.cse.ucsc.edu/goldenPath/danRer10/database/chromInfo.txt.gz";
            default -> null; // should never happen
        };
    }

    @Override
    public void setChosenRestrictionEnzymes(List<RestrictionEnzyme> chosenEnzymes) {
        model.setChosenRestrictionEnzymes(chosenEnzymes);
    }

    @Override
    public List<RestrictionEnzyme> getAllEnyzmes() {
        return model.getRestrictionEnymes();
    }

    @Override
    public List<RestrictionEnzyme> getSelectedEnyzmes() {
        return model.getChosenEnzymelist();
    }

    @Override
    public void setTargetType(GopherModel.TargetType targetType) {
        model.setTargetType(targetType);
    }

    @Override
    public void setTargetGenesPath(String absolutePath) {
        model.setTargetGenesPath(absolutePath);
    }

    @Override
    public void setN_validGeneSymbols(int size) {
        model.setN_validGeneSymbols(size);
    }

    @Override
    public void setUniqueTSScount(int uniqueTSSpositions) {
        model.setUniqueTSScount(uniqueTSSpositions);
    }

    @Override
    public void setUniqueChosenTSScount(int uniqueChosenTSS) {
        model.setUniqueChosenTSScount(uniqueChosenTSS);
    }

    @Override
    public void setChosenGeneCount(int chosenGeneCount) {
        model.setChosenGeneCount(chosenGeneCount);
    }

    @Override
    public void setTotalRefGeneCount(int n_genes) {
        model.setTotalRefGeneCount(n_genes);
    }


    @Override
    public void setGopherGenes(List<GopherGene> gopherGeneList) {
        model.setGopherGenes(gopherGeneList);
    }

    @Override
    public List<ViewPoint> getViewPointList() {
        return model.getViewPointList();
    }

    @Override
    public String getProjectName() {
        return getProjectName(false);
    }
    @Override
    public String getProjectName(boolean removeSuffix) {
        String projectName = model.getProjectName();
        if (projectName == null) return "";
        if (removeSuffix) {
            projectName = projectName.replace(".ser", "");
        }
        return projectName;
    }

    @Override
    public String getProjectTitle() {
        String projectName = getProjectName(true);
        return "Gopher: " + projectName;
    }

    @Override
    public String getGenomeFastaFile() {
        return model.getGenomeFastaFile();
    }

    @Override
    public String getIndexedGenomeFastaIndexFile() {
        return model.getIndexedGenomeFastaIndexFile();
    }

    @Override
    public void setGenomeIndexed(boolean indexed) {
        if (indexed) model.setGenomeIndexed();
    }

    @Override
    public void setApproach(String approach) {
        model.setApproach(approach);
    }

    @Override
    public List<GopherGene> getGopherGeneList() {
        if (model.getGopherGeneList() == null) return List.of();
        return model.getGopherGeneList();
    }

    @Override
    public String getHttpProxyPort() {
        return model.getHttpProxyPort();
    }

    @Override
    public String getHttpProxy() {
        return model.getHttpProxy();
    }

    @Override
    public void setHttpProxy(String proxy) {
        model.setHttpProxy(proxy);
    }

    @Override
    public void setHttpProxyPort(String port) {
        model.setHttpProxyPort(port);
    }

    @Override
    public String getLastChangeDate() {
        return model.getLastChangeDate();
    }

    @Override
    public void setRegulatoryBuildPath(String abspath) {
        model.setRegulatoryBuildPath(abspath);
    }

    @Override
    public boolean viewpointsInitialized() {
        return model.viewpointsInitialized();
    }

    @Override
    public boolean regulatoryBuildPathInitialized() {
        return model.regulatoryBuildPathInitialized();
    }

    @Override
    public boolean isClean() {
        return model.isClean();
    }

    @Override
    public void setAllowPatching(boolean b) {
        model.setAllowPatching(b);
    }

    @Override
    public void setAllowUnbalancedMargins(boolean b) {
        model.setAllowUnbalancedMargins(b);
    }

    @Override
    public int getMaxMeanKmerAlignability() {
        return model.getMaxMeanKmerAlignability();
    }

    @Override
    public double getMinGCContentPercent() {
        return model.getMinGCContentPercent();
    }

    @Override
    public double getMaxGCContentPercent() {
        return model.getMaxGCContentPercent();
    }

    @Override
    public int getMinFragSize() {
        return model.getMinFragSize();
    }

    @Override
    public String getAlignabilityMapPathIncludingFileNameGz() {
        return model.getAlignabilityMapPathIncludingFileNameGz();
    }

    @Override
    public String getTargetGenesPath() {
        return model.getTargetGenesPath();
    }

    @Override
    public String getTranscriptsBasename() {
        return model.getTranscriptsBasename();
    }

    @Override
    public String getGenomeDirectoryPath() {
        return model.getGenomeDirectoryPath();
    }

    @Override
    public Properties getRegulatoryExomeProperties() {
        return model.getRegulatoryExomeProperties();
    }

    @Override
    public int getMinBaitCount() {
        return model.getMinBaitCount();
    }

    @Override
    public double getMaxGCcontent() {
        return model.getMaxGCcontent();
    }

    @Override
    public double getMinGCcontent() {
        return model.getMinGCcontent();
    }

    @Override
    public List<String> initializeEntrezGene(String pathToEntrezGeneFile) {
        List<String> symbols = new ArrayList<>();
        File f = new File(pathToEntrezGeneFile);
        if (! f.isFile()) {
            LOGGER.error("Could not find entrez Gene file at {}", f.getAbsoluteFile());
            return List.of();
        }
        this.model.setTargetGenesPath(f.getAbsolutePath());

        try {
            BufferedReader br =new BufferedReader(new FileReader(f));
            String line;
            while ((line=br.readLine())!=null) {
                symbols.add(line.trim());
            }
        } catch (IOException err) {
            LOGGER.error("I/O Error reading file with target genes: {}", err.getMessage());
        }
        return symbols;
    }

    @Override
    public void setEstAvgRestFragLen(double estAvgRestFragLen) {
        model.setEstAvgRestFragLen(estAvgRestFragLen);
    }

    @Override
    public void importProjectFromFile(File file) {
        LOGGER.info("Importing GOPHER model from {}", file.getAbsoluteFile());
        try {
            this.model = SerializationManager.deserializeModel(file.getAbsolutePath());
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error("Could not import model from {}", file.getAbsoluteFile());
        }
    }

    @Override
    public List<ViewPoint> getActiveViewPointList() {
        return model.getActiveViewPointList();
    }

    @Override
    public double getMaxRepeatContent() {
        return model.getMaxRepeatContent();
    }

    @Override
    public NormalDistribution getNormalDistributionExtendedUp() {
        return model.getNormalDistributionExtendedUp();
    }

    @Override
    public NormalDistribution getNormalDistributionExtendedDown() {
        return model.getNormalDistributionExtendedDown();
    }
    @Override
    public NormalDistribution getNormalDistributionSimple() {
        return model.getNormalDistributionSimple();
    }

    @Override
    public Double getEstAvgRestFragLen() {
        return model.getEstAvgRestFragLen();
    }

    @Override
    public void setNormalDistributionSimple(double meanLen) {
        model.setNormalDistributionSimple(meanLen);
    }

    @Override
    public String getChromInfoPathIncludingFileNameGz() {
        return model.getChromInfoPathIncludingFileNameGz();
    }

    @Override
    public void setViewPoints(List<ViewPoint> viewpointlist) {
        model.setViewPoints(viewpointlist);
    }

    @Override
    public String getRegulatoryBuildPath() {
        return model.getRegulatoryBuildPath();
    }

    @Override
    public void setRegulatoryExomeProperties(Properties regulatoryProperties) {
        model.setRegulatoryExomeProperties(regulatoryProperties);
    }

    @Override
    public void setNormalDistributionsExtended() {
        model.setNormalDistributionsExtended();
    }

    @Override
    public void deleteViewpoint(ViewPoint viewpoint) {
        model.deleteViewpoint(viewpoint);
    }

    @Override
    public int getTotalRefGeneCount() {
        return model.getTotalRefGeneCount();
    }

    @Override
    public int getChosenGeneCount() {
        return model.getChosenGeneCount();
    }

    @Override
    public int getUniqueChosenTSScount() {
        return model.getUniqueChosenTSScount();
    }

    @Override
    public int getUniqueTSScount() {
        return model.getUniqueTSScount();
    }

    /**
     * Called when the user clicks on the get target genes button for loading a list of gene symbols.
     * TODO The HTML display code does not belong here and should be refactored.
     * @param f file with target genes
     */
    @Override
    public void getTargetGopherGenesFromFile(File f){
        List<String> symbols= new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String [] fields = line.split("\t");
                symbols.add(fields[0]);
            }
        } catch (IOException e) {
            PopupFactory.displayError("Error reading target genes",e.getMessage());
            return;
        }
        String path = this.model.getRefGenePath();
        if (path==null) {
            LOGGER.error("Attempt to validate gene symbols before refGene.txt.gz file was downloaded");
            PopupFactory.displayError("Error retrieving refGene data","Download refGene.txt.gz file before proceeding.");
            return;
        }
        LOGGER.info("About to parse refGene.txt.gz file to validate uploaded gene symbols. Path at "+ path);
        RefGeneParser parser;
        try {
            parser = new RefGeneParser(path);
            parser.checkGenes(symbols);
        } catch (Exception exc) {
            PopupFactory.displayException("Error while attempting to validate Gene symbols","Could not validate gene symbols",exc);
            return;
        }
        List<String>  validGeneSymbols = parser.getValidGeneSymbols();
        List<String> invalidGeneSymbols= parser.getInvalidGeneSymbols();

        int uniqueTSSpositions = parser.getTotalTSScount();
        int n_genes=parser.getTotalNumberOfRefGenes();
        int chosenGeneCount=parser.getNumberOfRefGenesChosenByUser();
        int uniqueChosenTSS=parser.getCountOfChosenTSS();
        String html = getValidatedGeneListHTML(validGeneSymbols, invalidGeneSymbols,n_genes, uniqueTSSpositions);
        this.model.setN_validGeneSymbols(validGeneSymbols.size());
        this.model.setUniqueTSScount(uniqueTSSpositions);
        this.model.setUniqueChosenTSScount(uniqueChosenTSS);
        this.model.setChosenGeneCount(chosenGeneCount);
        this.model.setGopherGenes(parser.getGopherGeneList());
        model.setTotalRefGeneCount(n_genes);
        PopupFactory.displayHtml(html, "Target genes");
    }

    private String getValidatedGeneListHTML(List<String> valid, List<String> invalid, int n_genes, int n_transcripts) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("""
                <style type="text/css">
                 span.bold-red {
                    color: red;
                    font-weight: bold;
                }
                 span.blu {
                    color: #4e89a4;
                    font-weight: normal;
                }
                </style>""");
        sb.append("<body><h3>Validated gene list</h3>");
        sb.append(String.format("<p>We parsed a total number of %d genes and found a total of %d associated transcripts.</p>",n_genes,n_transcripts));
        sb.append(String.format("<p>%d of the uploaded gene symbols were valid, and %d were invalid or could not be parsed.</p>",
                valid.size(),invalid.size()));
        if (invalid.size()>0) {
            sb.append("<p>Invalid genes:<br/>");
            for (String inv : invalid) {
                sb.append("<span class=\"bold-red\">").append(inv).append("</span><br/>");
            }
            sb.append("</p>");
            sb.append("<p><i>Please correct the gene symbol in the input file before proceeding. ");
            sb.append("Otherwise, the invalid gene symbol will be ignored.</i></p>");
        }
        if (valid.size()==0) {
            sb.append("<p>Error: No valid genes found!</p>");
        } else {
            sb.append("<p>Valid genes:<br/>");
            for (String v : valid) {
                sb.append("<span class=\"blu\">").append(v).append("</span><br/>");
            }
            sb.append("</p>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

}
