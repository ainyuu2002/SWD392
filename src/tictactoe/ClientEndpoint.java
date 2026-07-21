package tictactoe;

/**
 * Boundary abstraction the coordinator depends on (SOLID-D): a client connection
 * that can be messaged and closed. Implemented by {@link PlayerSession}.
 * Lets {@link RoomManager} depend on an interface instead of the concrete socket class.
 */
public interface ClientEndpoint extends MessageChannel {
    void close();
}
