package tictactoe;

/** Entity: the 3x3 grid. Pure data, no I/O. */
public final class Board {
    private final Mark[][] cells = new Mark[3][3];

    public Board() { clear(); }

    public void clear() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                cells[r][c] = Mark.EMPTY;
    }

    public boolean inRange(int r, int c) {
        return r >= 0 && r < 3 && c >= 0 && c < 3;
    }

    public boolean isEmpty(int r, int c) { return cells[r][c] == Mark.EMPTY; }

    public void place(int r, int c, Mark mark) { cells[r][c] = mark; }

    public Mark at(int r, int c) { return cells[r][c]; }

    public boolean isFull() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                if (cells[r][c] == Mark.EMPTY) return false;
        return true;
    }

    /** Row-major 9-char snapshot, e.g. "X.O.X...O", used by the wire protocol. */
    public String serialize() {
        StringBuilder sb = new StringBuilder(9);
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                sb.append(cells[r][c].symbol());
        return sb.toString();
    }
}
