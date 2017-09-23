package vpvgui.consoletest;


public class TranscriptionStartSite {

    private String referenceSequenceID;
    private Integer pos;
    private String strand;

    public TranscriptionStartSite(String referenceSequenceID, Integer pos, String strand) {
        this.referenceSequenceID=referenceSequenceID;
        this.pos=pos;
        this.strand=strand;
    }

    public final String getReferenceSequenceID() {
        return referenceSequenceID;
    }

    public final Integer getPos() {
        return pos;
    }

    public final String getStrand() {
        return strand;
    }

}


