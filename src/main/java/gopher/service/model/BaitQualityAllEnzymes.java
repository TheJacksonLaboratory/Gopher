package gopher.service.model;

import gopher.exception.GopherException;
import gopher.exception.GopherRuntimException;
import gopher.io.RestrictionEnzymeParser;
import gopher.service.GopherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

public class BaitQualityAllEnzymes {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaitQualityAllEnzymes.class);

    private final List<RestrictionEnzyme> restrictionEnzymeList;

    @Autowired
    private GopherService service;

    public BaitQualityAllEnzymes(){
        LOGGER.error("BaitQualityAllEnzymes");
        try {
            restrictionEnzymeList = RestrictionEnzymeParser.getEnzymes(GopherModel.class.getResourceAsStream("/data/enzymelist.tab"));
        } catch (IOException e) {
            throw new GopherRuntimException("Could not initialize enzyme list");
        }
        for (var enzyme : restrictionEnzymeList) {
            service.setProjectName(enzyme.getName());
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
        }
    }

    public void run() {

    }



}
