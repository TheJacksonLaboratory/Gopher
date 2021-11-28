package gopher.service.model.regulatoryexome;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encapsulates one line in what will become the BED file to order the regulatory exome/gene panel. We
 * model this data with a class so that we can sort the BED file according to chromosomal location after we have
 * extracted all the areas to be enriched.
 *
 * @author Peter Robinson
 * @version 0.1.3 (2017-11-14)
 */
public class RegulatoryBEDFileEntry implements Comparable<RegulatoryBEDFileEntry> {
    private static Logger logger = LoggerFactory.getLogger(RegulatoryBEDFileEntry.class.getName());
    private final String chromosome;
    private final int fromPos;
    private final int toPos;
    private final String elementName;

    RegulatoryBEDFileEntry(String chrom, int from, int to, String name) {
        if (!chrom.startsWith("chr"))
            chrom = String.format("chr%s", chrom); // coming from Ensembl and does not have chr at start of chromosomes
        this.chromosome = chrom;
        this.fromPos = from;
        this.toPos = to;
        this.elementName = name;
    }

    RegulatoryBEDFileEntry(RegulatoryElement regelem) {
        this.chromosome = regelem.getChrom();
        this.fromPos = regelem.getFrom();
        this.toPos = regelem.getTo();
        this.elementName = String.format("%s[%s]", regelem.getId(), regelem.getCategory());
    }


    public String toString() {
        return String.format("%s\t%d\t%d\t%s", chrom2string(chromosome), fromPos, toPos, elementName);
    }

    /**
     * Add a "chr" if needed to the beginning of the string.
     */
    private String chrom2string(String c) {
        if (c.startsWith("chr")) return c;
        else return (String.format("chr%s", c));
    }


    private int chromAsInt() {
        String chr = chromosome.replaceAll("chr", "");
        if (chr.equalsIgnoreCase("X")) return 23;
        else if (chr.equals("Y")) return 24;
        else if (chr.equals("MT")) return 25;
        else {
            try {
                return Integer.parseInt(chr);
            } catch (NumberFormatException e) {
                logger.error(String.format("Integer parse error for chromosome \"%s\"", chromosome));
                return 42;// should never happen
            }
        }
    }


    /**
     * Hash code is based on the end and start positions as well as on the chromosome.
     * THe element name is not regarded as relevant for equality.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 17;
        result = prime * result + chromosome.hashCode();
        result = prime * result + fromPos;
        result = prime * result + toPos;
        result = prime * result + elementName.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof RegulatoryBEDFileEntry oth)) return false;
        return chromosome.equals(oth.chromosome) &&
                fromPos == oth.fromPos &&
                toPos == oth.toPos &&
                elementName.equals(oth.elementName);
    }

    @Override
    public int compareTo(RegulatoryBEDFileEntry other) {
        int chromcomp = this.chromAsInt() - other.chromAsInt();
        int fromcomp = this.fromPos - other.fromPos;
        if (chromcomp == 0) {
            return fromcomp;
        } else {
            return chromcomp;
        }
    }

    public String getChromosome() {
        return chromosome;
    }

    public int getFromPos() {
        return fromPos;
    }

    public int getToPos() {
        return toPos;
    }

    /**
     * @param that another RegulatoryBEDFileENtry
     * @return true iff this overlaps that
     */
    public boolean overlaps(RegulatoryBEDFileEntry that) {
        if (that == null) return false;
        else if (!this.chromosome.equals(that.chromosome)) return false;
        else if (that.fromPos >= this.fromPos && that.fromPos <= this.toPos) return true;
        else if (that.toPos >= this.fromPos && that.toPos <= this.toPos) return true;
        else return false;
    }

}
