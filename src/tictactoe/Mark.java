package tictactoe;

/** Entity: a cell/player mark. */
public enum Mark {
    EMPTY('.'), X('X'), O('O');

    private final char symbol;

    Mark(char symbol) { this.symbol = symbol; }

    public char symbol() { return symbol; }

    /** The other playing mark (EMPTY has no opponent). */
    public Mark opponent() {
        if (this == X) return O;
        if (this == O) return X;
        return EMPTY;
    }
}
