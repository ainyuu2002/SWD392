package tictactoe.game;

public enum Mark {
    EMPTY('.'), X('X'), O('O');

    private final char symbol;

    Mark(char symbol) {
        this.symbol = symbol;
    }

    public char symbol() {
        return symbol;
    }

    public Mark opponent() {
        if (this == X) return O;
        if (this == O) return X;
        return EMPTY;
    }
}
