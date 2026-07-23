package tictactoe.game;

public final class Board {
    public enum MoveError { OUT_OF_RANGE, CELL_OCCUPIED }
    public enum Outcome { IN_PROGRESS, X_WINS, O_WINS, DRAW }

    private final Mark[][] cells = new Mark[3][3];

    public Board() {
        reset();
    }

    public void reset() {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                cells[row][col] = Mark.EMPTY;
    }

    public MoveError place(int row, int col, Mark mark) {
        int cellRow = row - 1;
        int cellCol = col - 1;
        if (!inRange(cellRow, cellCol)) return MoveError.OUT_OF_RANGE;
        if (cells[cellRow][cellCol] != Mark.EMPTY) return MoveError.CELL_OCCUPIED;
        cells[cellRow][cellCol] = mark;
        return null;
    }

    public Outcome outcome() {
        Mark winner = winner();
        if (winner == Mark.X) return Outcome.X_WINS;
        if (winner == Mark.O) return Outcome.O_WINS;
        return isFull() ? Outcome.DRAW : Outcome.IN_PROGRESS;
    }

    public String serialize() {
        StringBuilder snapshot = new StringBuilder(9);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                snapshot.append(cells[row][col].symbol());
        return snapshot.toString();
    }

    private Mark winner() {
        for (int i = 0; i < 3; i++) {
            if (isLine(cells[i][0], cells[i][1], cells[i][2])) return cells[i][0];
            if (isLine(cells[0][i], cells[1][i], cells[2][i])) return cells[0][i];
        }
        if (isLine(cells[0][0], cells[1][1], cells[2][2])) return cells[0][0];
        if (isLine(cells[0][2], cells[1][1], cells[2][0])) return cells[0][2];
        return Mark.EMPTY;
    }

    private boolean isLine(Mark first, Mark second, Mark third) {
        return first != Mark.EMPTY && first == second && second == third;
    }

    private boolean isFull() {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                if (cells[row][col] == Mark.EMPTY) return false;
        return true;
    }

    private boolean inRange(int row, int col) {
        return row >= 0 && row < 3 && col >= 0 && col < 3;
    }
}
