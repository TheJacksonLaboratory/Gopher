package gopher.service.model;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * @author Peter Robinson
 * @version 0.2.1
 */
public class RestrictionEnzyme implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestrictionEnzyme.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 1L;
    /** A name of a restirction enzyme, something like HindIII */
    private String name;
    /** A representation of the cutting site of the enzyme, whereby "^" stands for cut here.
     * For instance, A^AGCTT is the cutting site for HindIII.
     */
    private String site;

    /** Same as site but without the caret symbol */
    private String plainSite;
    /** The offset of the cutting site in this restriction enzyme. For instancen the offset for ^GATC is 0 and the
     * offset for A^AGCTT is 1.
     */
    private final Integer offset;

    public RestrictionEnzyme(String n, String s) {
        name=n;
        site=s;
        this.offset=site.indexOf('^');
        if (offset<0) {
            LOGGER.error(String.format("Malformed site pattern for enyze %s (%s)",name,site)); /* Should never happen!*/
        }
        plainSite=site;
        int i= site.indexOf('^');
        if (i>=0) {
            plainSite=site.substring(0,i)+site.substring(i+1);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSite() {
        return site;
    }

    public String getPlainSite() { return  plainSite; }

    public void setSite(String site) {
        this.site = site;
    }

    public String getLabel() {
        return String.format("%s: %s",getName(),getSite());
    }

    public Integer getOffset() { return this.offset; }
}
