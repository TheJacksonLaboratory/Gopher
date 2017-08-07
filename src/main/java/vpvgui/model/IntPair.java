package vpvgui.model;

public class IntPair {
    int startPos;
    int endPos;

    public IntPair(int start, int end) {
        this.startPos=start;
        this.endPos=end;
    }

    public int getStartPos() {
        return startPos;
    }

    public int getEndPos() {
        return endPos;
    }

    public int length() {
        return endPos-startPos+1;
    }
}
