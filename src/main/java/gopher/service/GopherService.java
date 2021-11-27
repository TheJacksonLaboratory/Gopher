package gopher.service;

import gopher.model.Approach;
import gopher.model.GopherGene;
import gopher.model.Model;
import gopher.model.RestrictionEnzyme;
import gopher.model.genome.Genome;
import gopher.model.viewpoint.ViewPoint;
import javafx.scene.control.ProgressIndicator;

import java.net.URL;
import java.util.Collection;
import java.util.List;

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

    void setModel(Model mod);

    int getSizeUp();
    int getSizeDown();

    int getProbeLength();

    int getMarginSize();

    Model.TargetType getTargetType();
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

    void setTargetType(Model.TargetType targetGenes);

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
}
