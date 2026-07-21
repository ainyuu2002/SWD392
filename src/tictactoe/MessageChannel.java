package tictactoe;

/**
 * Interface (SOLID-D): the only seam between game logic and the transport.
 * A real implementation writes to a socket; a test can use an in-memory stub.
 */
public interface MessageChannel {
    void send(String message);
}
