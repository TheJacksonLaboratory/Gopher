package gopher.service.model;

import gopher.exception.GopherRuntimeException;
import gopher.gui.factories.PopupFactory;
import gopher.gui.factories.QCCheckFactory;
import gopher.io.RefGeneParser;
import gopher.io.RestrictionEnzymeParser;
import gopher.service.GopherService;
import gopher.service.impl.GopherServiceImpl;
import gopher.service.model.viewpoint.SimpleViewPointCreationTask;
import gopher.service.model.viewpoint.ViewPointCreationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import java.io.IOException;
import java.util.List;


public class BaitQualityAllEnzymes {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaitQualityAllEnzymes.class);

    private final List<RestrictionEnzyme> restrictionEnzymeList;

    private GopherService service;

    public BaitQualityAllEnzymes(String refGenePath){
        LOGGER.info("BaitQualityAllEnzymes");
        try {
            restrictionEnzymeList = RestrictionEnzymeParser.getEnzymes(GopherModel.class.getResourceAsStream("/data/enzymelist.tab"));
        } catch (IOException e) {
            throw new GopherRuntimeException("Could not initialize enzyme list");
        }
        GopherModel model = new GopherModel();
        service = new GopherServiceImpl(model);
        service.setRefGenePath(refGenePath);

        RefGeneParser parser;
        try {
            String path = this.service.getRefGenePath();
            parser = new RefGeneParser(path);
        } catch (Exception exc) {
            PopupFactory.displayException("Error while attempting to validate Gene symbols", "Could not validate gene symbols", exc);
            return;
        }
        List<String> validGeneSymbols = parser.getAllProteinCodingGeneSymbols();
        int uniqueTSSpositions = parser.getTotalTSScount();
        int n_genes = parser.getTotalNumberOfRefGenes();
        int chosenGeneCount = parser.getNumberOfRefGenesChosenByUser();
        int uniqueChosenTSS = parser.getCountOfChosenTSS();
        this.service.setN_validGeneSymbols(validGeneSymbols.size());
        this.service.setUniqueTSScount(uniqueTSSpositions);
        this.service.setUniqueChosenTSScount(uniqueChosenTSS);
        this.service.setChosenGeneCount(chosenGeneCount);
        this.service.setTotalRefGeneCount(n_genes);
        this.service.setGopherGenes(parser.getGopherGeneList());
        this.service.setUniqueChosenTSScount(parser.getCountOfChosenTSS());
        this.service.setTargetType(GopherModel.TargetType.ALL_GENES);
        LOGGER.info("Got {} protein-coding TSS", service.getUniqueChosenTSScount());
        QCCheckFactory qcFactory = new QCCheckFactory(this.service);
    }

    public void run() {
        for (var enzyme : restrictionEnzymeList) {
            service.setProjectName(enzyme.getName());
            service.setChosenRestrictionEnzymes(List.of(enzyme));
            // Set defaults -- but allow digests with zero baits here, we are calculate statistics
            service.setSizeDown(Default.SIZE_DOWNSTREAM);
            this.service.setSizeUp(Default.SIZE_UPSTREAM);
            this.service.setMinFragSize(Default.MINIMUM_FRAGMENT_SIZE);
            this.service.setMaxRepeatContent(Default.MAXIMUM_KMER_ALIGNABILITY);
            this.service.setMinGCcontent(Default.MIN_GC_CONTENT);
            this.service.setMaxGCcontent(Default.MAX_GC_CONTENT);
            this.service.setMaxMeanKmerAlignability(Default.MAXIMUM_KMER_ALIGNABILITY);
            this.service.setMinBaitCount(0);
            this.service.setProbeLength(Default.BAIT_LENGTH);
            this.service.setMarginSize(Default.MARGIN_SIZE);
            ViewPointCreationTask task = new SimpleViewPointCreationTask(this.service);
            task.setOnSucceeded(event -> {
                LOGGER.info("View Point Creation Task succeded");

            });
            task.setOnFailed(eh -> {
                LOGGER.error("Exception encountered while attempting to create viewpoints");
            });
            LOGGER.info("Starting task");
            new Thread(task).start();
            task.run();
            // break;
        }



    }



}
