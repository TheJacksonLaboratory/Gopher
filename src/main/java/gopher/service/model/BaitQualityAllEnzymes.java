package gopher.service.model;

import gopher.exception.GopherException;
import gopher.exception.GopherRuntimException;
import gopher.io.RestrictionEnzymeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class BaitQualityAllEnzymes {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaitQualityAllEnzymes.class);

    private final List<RestrictionEnzyme> restrictionEnzymeList;

    public BaitQualityAllEnzymes(){
        LOGGER.error("BaitQualityAllEnzymes");
        try {
            restrictionEnzymeList = RestrictionEnzymeParser.getEnzymes(GopherModel.class.getResourceAsStream("/data/enzymelist.tab"));
        } catch (IOException e) {
            throw new GopherRuntimException("Could not initialize enzyme list");
        }
    }

    public void run() {

    }



}
