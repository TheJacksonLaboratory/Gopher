package gopher.service.model;

public enum Approach {
    SIMPLE, EXTENDED, UNINITIALIZED;
    public String toString() {
        switch (this) {
            case SIMPLE:
                return "simple";
            case EXTENDED:
                return "extended";
            case UNINITIALIZED:
            default:
                return "uninitialized";
        }
    }
}
