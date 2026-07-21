package tictactoe;

/** Application logic (algorithm): validates a move without UI/protocol concerns. */
public class MoveValidator {
    public MoveError validate(Board board, Mark turn, Mark playerSymbol, int row, int col) {
        if (playerSymbol != turn) return MoveError.NOT_YOUR_TURN;
        int r = row - 1;
        int c = col - 1;
        if (!board.inRange(r, c)) return MoveError.OUT_OF_RANGE;
        if (!board.isEmpty(r, c)) return MoveError.CELL_OCCUPIED;
        return null;
    }
}
