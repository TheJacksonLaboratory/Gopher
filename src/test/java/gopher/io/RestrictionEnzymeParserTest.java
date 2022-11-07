package gopher.io;

import gopher.service.model.GopherModel;
import gopher.service.model.RestrictionEnzyme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RestrictionEnzymeParserTest {
    private static List<RestrictionEnzyme> restrictionEnzymeList;

    @BeforeAll
    public static void init() throws IOException {
        restrictionEnzymeList =
                RestrictionEnzymeParser.getEnzymes(GopherModel.class.getResourceAsStream("/data/enzymelist.tab"));
    }

    @Test
    public void testNumberOfEnzymesParsed() {
        final int expectedNumberOfEnzymes = 19; // from file enzymelist.tab
        assertEquals(expectedNumberOfEnzymes, restrictionEnzymeList.size());
    }

    /**
     * NlaIII  CATG^
     */
    @Test
    public void testContainsNlaIII() {
        Optional<RestrictionEnzyme> opt =
                restrictionEnzymeList.stream().filter(re -> "NlaIII".equals(re.getName())).findAny();
        assertTrue(opt.isPresent());
        RestrictionEnzyme re = opt.get();
        assertEquals("NlaIII", re.getName());
        assertEquals("CATG^", re.getSite());
        assertEquals("CATG", re.getPlainSite());
    }



}
