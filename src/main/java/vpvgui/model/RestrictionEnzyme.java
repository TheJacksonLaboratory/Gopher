package vpvgui.model;

import org.apache.log4j.Logger;

import java.io.Serializable;

/**
 * Created by robinp on 5/11/17.
 */
public class RestrictionEnzyme implements Serializable {
    static Logger logger = Logger.getLogger(RestrictionEnzyme.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 1L;
    /** A name of a restirction enzyme, something like HindIII */
    private String name;
    /** A representation of the cutting site of the enzyme, whereby "^" stands for cut here.
     * For instance, A^AGCTT is the cutting site for HindIII.
     */
    private String site;

    public RestrictionEnzyme(String n, String s) {
        name=n;
        site=s;
        //logger.trace(String.format("CTOR: \"%s\", \"%s\"",name,site));
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

    public void setSite(String site) {
        this.site = site;
    }

    public String getLabel() {
        return String.format("%s: %s",getName(),getSite());
    }
}
