package gopher.service.model;

public enum Approach {
    SIMPLE, EXTENDED, UNINITIALIZED;
    public String toString() {
        return switch (this) {
            case SIMPLE -> "simple";
            case EXTENDED -> "extended";
            case UNINITIALIZED -> "uninitialized";
        };
    }
}
