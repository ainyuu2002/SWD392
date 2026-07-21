package tictactoe;

/** Shared line-based protocol tokens (client and server). */
public final class Protocol {
    private Protocol() {}

    // client -> server
    public static final String JOIN = "JOIN";
    public static final String INVITE = "INVITE";
    public static final String ACCEPT = "ACCEPT";
    public static final String DECLINE = "DECLINE";
    public static final String MOVE = "MOVE";
    public static final String AGAIN = "AGAIN";
    public static final String QUIT = "QUIT";

    // server -> client
    public static final String MSG = "MSG";
    public static final String BOARD = "BOARD";
    public static final String SYMBOL = "SYMBOL";
    public static final String PROMPT = "PROMPT";
    public static final String BYE = "BYE";

    // PROMPT kinds
    public static final String P_NAME = "NAME";
    public static final String P_INVITE = "INVITE";
    public static final String P_ACCEPT = "ACCEPT";
    public static final String P_MOVE = "MOVE";
    public static final String P_AGAIN = "AGAIN";
}
