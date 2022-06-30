package gopher.service.model;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by peter on 29.05.17.
 */
public class RestrictionEnzymeTest {

    private static RestrictionEnzyme re;

    @BeforeAll
    public static void setup() {
        re = new RestrictionEnzyme("HindIII", "A^AGCTT");
    }
    @Test
    public void testGetName() {
        String expected="HindIII";
        assertEquals(expected,re.getName());
    }
    @Test
    public void testGetRestrictionSite() {
        String expected="A^AGCTT";
        assertEquals(expected,re.getSite());
    }
}
