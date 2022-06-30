package gopher.service.model;

/**
 * Simple class with a pair of integers (start and end) with getters and a length function.
 */
public record IntPair(int startPos, int endPos) {
    public int length() {
        return endPos-startPos+1;
    }
}
