package gopher.service;

import gopher.service.model.Approach;
import gopher.service.model.GopherGene;
import gopher.service.model.GopherModel;
import gopher.service.model.RestrictionEnzyme;
import gopher.service.model.genome.Genome;
import gopher.service.model.viewpoint.ViewPoint;
import javafx.scene.control.ProgressIndicator;

import java.util.List;
import java.util.Properties;

public interface GopherService {

    boolean serialize();
    boolean serializeToLocation(String path);
    boolean isGenomeUnpacked();
    boolean isGenomeIndexed();
    String getRefGenePath();
    boolean alignabilityMapPathIncludingFileNameGzExists();

    List<RestrictionEnzyme> getChosenEnzymelist();

    String getAllSelectedEnzymeString();

    boolean getAllowUnbalancedMargins();
    boolean getAllowPatching();


    void setProjectName(String projectname);

    void setModel(GopherModel mod);

    int getSizeUp();
    int getSizeDown();

    int getProbeLength();

    int getMarginSize();

    GopherModel.TargetType getTargetType();
    Approach getApproach();

    int getN_validGeneSymbols();

    boolean useExtendedApproach();
    boolean useSimpleApproach();

    String getGenomeBuild();

    void setClean(boolean b);

    void setMarginSize(int marginsize);

    void setProbeLength(int baitlen);

    void setMinBaitCount(int minbait);

    void setMaxGCcontent(double v);

    void setMaxMeanKmerAlignability(int kmerAlign);

    void setSizeDown(int i);

    void setSizeUp(int i);

    void setMinFragSize(int i);

    void setMaxRepeatContent(double v);

    void setMinGCcontent(double v);

    void setGenomeBuild(String build);

    boolean checkDownloadComplete(String absolutePath);

    void setGenomeDirectoryPath(String absolutePath);

    String getGenomeBasename();

    void setTranscriptsBasename(String transcriptName);

    void setRefGenePath(String abspath);

    Genome getGenome();

    void setGenomeUnpacked();

    void indexGenome(ProgressIndicator genomeIndexPI);

    void setAlignabilityMapPathIncludingFileNameGz(String alignabilityMapPathIncludingFileNameGz);

    void setChromInfoPathIncludingFileNameGz(String chromInfoPathIncludingFileNameGz);

    String getAlignabilityFtp(String genomeBuild);
    String getAlignabilityHttp(String genomeBuild);

    void setChosenRestrictionEnzymes(List<RestrictionEnzyme> chosenEnzymes);
    List<RestrictionEnzyme> getAllEnyzmes();
    List<RestrictionEnzyme> getSelectedEnyzmes();

    void setTargetType(GopherModel.TargetType targetGenes);

    void setTargetGenesPath(String absolutePath);

    void setN_validGeneSymbols(int size);

    void setUniqueTSScount(int uniqueTSSpositions);

    void setUniqueChosenTSScount(int uniqueChosenTSS);

    void setChosenGeneCount(int chosenGeneCount);

    void setTotalRefGeneCount(int n_genes);

    void setGopherGenes(List<GopherGene> gopherGeneList);

    List<ViewPoint> getViewPointList();

    String getProjectName();

    String getGenomeFastaFile();

    String getIndexedGenomeFastaIndexFile();

    void setApproach(String approach);

    List<GopherGene> getGopherGeneList();

    String getHttpProxyPort();

    String getHttpProxy();

    void setHttpProxy(String proxy);

    void setHttpProxyPort(String port);

    String getLastChangeDate();

    void setRegulatoryBuildPath(String abspath);

    boolean viewpointsInitialized();

    boolean regulatoryBuildPathInitialized();

    boolean isClean();

    void setAllowPatching(boolean b);

    void setAllowUnbalancedMargins(boolean b);

    int getMaxMeanKmerAlignability();

    double getMinGCContentPercent();

    double getMaxGCContentPercent();

    int getMinFragSize();

    String getAlignabilityMapPathIncludingFileNameGz();

    String getTargetGenesPath();

    String getTranscriptsBasename();

    String getGenomeDirectoryPath();

    Properties getRegulatoryExomeProperties();

    int getMinBaitCount();

    double getMaxGCcontent();

    double getMinGCcontent();
}
