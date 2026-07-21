package tictactoe;

/** Domain-event boundary from GameController to the session coordinator. */
public interface GameListener {
    void onBoardChanged(String cells);
    void onTurnChanged(PlayerInfo current, PlayerInfo waiting);
    void onMoveRejected(PlayerInfo mover, MoveError error);
    void onGameOver(Outcome outcome);
}
