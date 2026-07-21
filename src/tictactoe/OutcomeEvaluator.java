package tictactoe;

/** Application logic (algorithm): win/draw detection. */
public class OutcomeEvaluator {
    public Outcome evaluate(Board b) {
        Mark w = winner(b);
        if (w == Mark.X) return Outcome.X_WINS;
        if (w == Mark.O) return Outcome.O_WINS;
        if (b.isFull()) return Outcome.DRAW;
        return Outcome.IN_PROGRESS;
    }

    private Mark winner(Board b) {
        for (int i = 0; i < 3; i++) {
            if (line(b.at(i, 0), b.at(i, 1), b.at(i, 2))) return b.at(i, 0); // rows
            if (line(b.at(0, i), b.at(1, i), b.at(2, i))) return b.at(0, i); // cols
        }
        if (line(b.at(0, 0), b.at(1, 1), b.at(2, 2))) return b.at(0, 0);     // diag
        if (line(b.at(0, 2), b.at(1, 1), b.at(2, 0))) return b.at(0, 2);     // anti-diag
        return Mark.EMPTY;
    }

    private boolean line(Mark a, Mark b, Mark c) {
        return a != Mark.EMPTY && a == b && b == c;
    }
}
