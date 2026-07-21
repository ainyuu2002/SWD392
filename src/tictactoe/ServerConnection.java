package tictactoe;

import java.io.IOException;

/** Client-side server connection abstraction used by ClientController. */
public interface ServerConnection extends MessageChannel {
    String receive() throws IOException;
    void close();
}
