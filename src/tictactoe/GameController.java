package tictactoe;

/**
 * Control (state-dependent): drives a single game across the session. Enforces
 * turn order, validates moves, broadcasts the board, detects the outcome, and
 * swaps roles between games. It emits domain events through GameListener and
 * has no console, protocol or socket responsibility.
 */
public class GameController {
    private final Board board;
    private final MoveValidator validator;
    private final OutcomeEvaluator evaluator;
    private final PlayerInfo playerA;
    private final PlayerInfo playerB;
    private final GameListener listener;
    private Mark turn = Mark.X;
    private boolean over = false;

    /** Dependencies and players are supplied by the session coordinator. */
    public GameController(Board board,
                          MoveValidator validator,
                          OutcomeEvaluator evaluator,
                          PlayerInfo playerA,
                          PlayerInfo playerB,
                          GameListener listener) {
        this.board = board;
        this.validator = validator;
        this.evaluator = evaluator;
        this.playerA = playerA;
        this.playerB = playerB;
        this.listener = listener;
    }

    /** Begin a game. X always moves first. */
    public void startGame() {
        board.clear();
        turn = Mark.X;
        over = false;
        listener.onBoardChanged(board.serialize());
        listener.onTurnChanged(current(), waiting());
    }

    /** Swap X/O for the next game (previous O becomes X). */
    public void swapRoles() {
        playerA.setSymbol(playerA.symbol().opponent());
        playerB.setSymbol(playerB.symbol().opponent());
    }

    /** End the current game without a normal result (e.g. opponent left). */
    public void abort() { over = true; }

    public boolean isOver() { return over; }

    /** Apply a move requested by {@code mover} (row/col are 1-based). */
    public void applyMove(PlayerInfo mover, int row, int col) {
        if (over) return;
        MoveError error = validator.validate(board, turn, mover.symbol(), row, col);
        if (error != null) {
            listener.onMoveRejected(mover, error);
            return;
        }
        board.place(row - 1, col - 1, mover.symbol());
        listener.onBoardChanged(board.serialize());

        Outcome outcome = evaluator.evaluate(board);
        if (outcome == Outcome.IN_PROGRESS) {
            turn = turn.opponent();
            listener.onTurnChanged(current(), waiting());
        } else {
            over = true;
            listener.onGameOver(outcome);
        }
    }

    private PlayerInfo xPlayer() { return playerA.symbol() == Mark.X ? playerA : playerB; }
    private PlayerInfo oPlayer() { return playerA.symbol() == Mark.X ? playerB : playerA; }
    private PlayerInfo current() { return turn == Mark.X ? xPlayer() : oPlayer(); }
    private PlayerInfo waiting() { return turn == Mark.X ? oPlayer() : xPlayer(); }

}
