package tictactoe;

/** Domain reasons why a requested move cannot be applied. */
public enum MoveError {
    NOT_YOUR_TURN,
    OUT_OF_RANGE,
    CELL_OCCUPIED
}
