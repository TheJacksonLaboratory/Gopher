package gopher.io;

import gopher.model.viewpoint.ViewPoint;
import org.apache.log4j.Logger;

import java.util.HashMap;

/**
 * Created by hansep on 6/12/18.
 */
public class AlignabilityMapParser {
    private static final Logger logger = Logger.getLogger(AlignabilityMapParser.class.getName());

    /**
     * @param path Path to the {@code wgEncodeCrgMapabilityAlign100mer.bigWig} file.
     */
    public AlignabilityMapParser(String path) {
        parsebigWig(path);
    }

    private void parsebigWig(String path) {

    }

}
