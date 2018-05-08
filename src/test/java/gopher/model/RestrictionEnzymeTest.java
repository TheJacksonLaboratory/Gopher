package gopher.model;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by peter on 29.05.17.
 */
public class RestrictionEnzymeTest {

    private static RestrictionEnzyme re;

    @BeforeClass
    public static void setup() {
        re = new RestrictionEnzyme("HindIII", "A^AGCTT");
    }
    @Test
    public void testGetName() {
        String expected="HindIII";
        Assert.assertEquals(expected,re.getName());
    }
    @Test
    public void testGetRestrictionSite() {
        String expected="A^AGCTT";
        Assert.assertEquals(expected,re.getSite());
    }
}
