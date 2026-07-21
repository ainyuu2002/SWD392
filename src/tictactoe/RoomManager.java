package tictactoe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Control (coordinator): owns the single 2-player room. Handles join / invite /
 * accept, the game lifecycle, session scoring, play-again and disconnect.
 * Depends on the {@link ClientEndpoint} abstraction, not the concrete socket class.
 * All command handling is synchronized so game state is mutated one move at a time.
 */
public class RoomManager implements GameListener {

    private enum State { WAITING, IDLE, INVITE_PENDING, IN_GAME, GAME_OVER }

    private final List<ClientEndpoint> players = new ArrayList<>();
    private final Map<ClientEndpoint, String> names = new HashMap<>();
    private final ScoreBoard score = new ScoreBoard();
    private final Map<ClientEndpoint, Boolean> againAnswers = new HashMap<>();
    private final Random random = new Random();

    private State state = State.WAITING;
    private PlayerInfo info1, info2;     // persistent info for the two seats (symbols swap each game)
    private GameController game;

    /** Accept a new connection if the room has a free seat. */
    public synchronized boolean register(ClientEndpoint session) {
        if (players.size() >= 2) return false;
        players.add(session);
        return true;
    }

    public synchronized void onCommand(ClientEndpoint s, String line) {
        String[] p = line.trim().split("\\s+");
        if (p.length == 0 || p[0].isEmpty()) return;
        String cmd = p[0].toUpperCase();

        switch (cmd) {
            case Protocol.JOIN:    handleJoin(s, p.length > 1 ? p[1] : ""); break;
            case Protocol.INVITE:  handleInvite(s); break;
            case Protocol.ACCEPT:  handleAccept(s); break;
            case Protocol.DECLINE: handleDecline(s); break;
            case Protocol.MOVE:    handleMove(s, p); break;
            case Protocol.AGAIN:   handleAgain(s, p.length > 1 ? p[1] : ""); break;
            case Protocol.QUIT:    onDisconnect(s); break;
            default:               s.send(msg("Unknown command."));
        }
    }

    // ---- pairing ---------------------------------------------------------

    private void handleJoin(ClientEndpoint s, String name) {
        if (names.containsKey(s)) { s.send(msg("You have already joined.")); return; }
        if (name.isEmpty())   { s.send(msg("Name cannot be empty.")); s.send(prompt(Protocol.P_NAME)); return; }
        if (nameTaken(name))  { s.send(msg("Name already taken, choose another.")); s.send(prompt(Protocol.P_NAME)); return; }

        names.put(s, name);
        score.register(name);
        broadcast(msg(name + " joined the room."));

        if (bothJoined()) beginInvite();
        else s.send(msg("Waiting for another player to join..."));
    }

    private void beginInvite() {
        state = State.IDLE;
        ClientEndpoint inviter = players.get(0), invitee = players.get(1);
        inviter.send(msg("Both players are here. You may invite " + playerName(invitee) + "."));
        inviter.send(prompt(Protocol.P_INVITE));
        invitee.send(msg("Waiting for " + playerName(inviter) + " to invite you..."));
    }

    private void handleInvite(ClientEndpoint s) {
        if (state != State.IDLE || players.indexOf(s) != 0) { s.send(msg("You cannot invite right now.")); return; }
        state = State.INVITE_PENDING;
        ClientEndpoint invitee = players.get(1);
        invitee.send(msg(playerName(s) + " invited you to play."));
        invitee.send(prompt(Protocol.P_ACCEPT));
        s.send(msg("Invitation sent, waiting for a response..."));
    }

    private void handleAccept(ClientEndpoint s) {
        if (state != State.INVITE_PENDING || players.indexOf(s) != 1) { s.send(msg("Nothing to accept.")); return; }
        broadcast(msg(playerName(s) + " accepted. Starting the game."));
        startGame();
    }

    private void handleDecline(ClientEndpoint s) {
        if (state != State.INVITE_PENDING || players.indexOf(s) != 1) { s.send(msg("Nothing to decline.")); return; }
        broadcast(msg(playerName(s) + " declined the invitation."));
        beginInvite();
    }

    // ---- game ------------------------------------------------------------

    private void startGame() {
        ClientEndpoint s1 = players.get(0), s2 = players.get(1);
        if (game == null) {                              // first game of the session
            info1 = new PlayerInfo(playerName(s1));
            info2 = new PlayerInfo(playerName(s2));
            boolean s1IsX = random.nextBoolean();        // random X for the first game
            info1.setSymbol(s1IsX ? Mark.X : Mark.O);
            info2.setSymbol(s1IsX ? Mark.O : Mark.X);
            game = new GameController(
                    new Board(),
                    new MoveValidator(),
                    new OutcomeEvaluator(),
                    info1,
                    info2,
                    this);
        } else {
            game.swapRoles();                            // previous O becomes X (swap every game)
        }
        state = State.IN_GAME;
        PlayerInfo x = xInfo(), o = oInfo();
        broadcast(msg("New game! " + x.name() + " is X, " + o.name() + " is O."));
        endpointFor(x).send(Protocol.SYMBOL + " X");
        endpointFor(o).send(Protocol.SYMBOL + " O");
        game.startGame();
    }

    private void handleMove(ClientEndpoint s, String[] p) {
        if (state != State.IN_GAME || game == null) { s.send(msg("No game in progress.")); return; }
        PlayerInfo mover = infoFor(s);
        if (mover == null) return;
        int row, col;
        try {
            row = Integer.parseInt(p[1]);
            col = Integer.parseInt(p[2]);
        } catch (Exception e) {
            s.send(msg("Enter your move as two numbers: row col."));
            s.send(prompt(Protocol.P_MOVE));
            return;
        }
        game.applyMove(mover, row, col);
    }

    @Override
    public synchronized void onBoardChanged(String cells) {
        broadcast(Protocol.BOARD + " " + cells);
    }

    @Override
    public synchronized void onTurnChanged(PlayerInfo current, PlayerInfo waiting) {
        endpointFor(current).send(prompt(Protocol.P_MOVE));
        endpointFor(waiting).send(msg("Opponent's turn, please wait..."));
    }

    @Override
    public synchronized void onMoveRejected(PlayerInfo mover, MoveError error) {
        ClientEndpoint endpoint = endpointFor(mover);
        endpoint.send(msg(moveErrorMessage(error)));
        endpoint.send(prompt(Protocol.P_MOVE));
    }

    /** GameController callback when a game finishes. */
    @Override
    public synchronized void onGameOver(Outcome outcome) {
        if (outcome == Outcome.DRAW) {
            broadcast(msg("It's a draw."));
            score.recordDraw();
        } else {
            PlayerInfo winner = outcome == Outcome.X_WINS ? xInfo() : oInfo();
            PlayerInfo loser = outcome == Outcome.X_WINS ? oInfo() : xInfo();
            endpointFor(winner).send(msg("You win!"));
            endpointFor(loser).send(msg("You lose."));
            score.recordWin(winner.name());
        }

        broadcast(msg(score.standings()));
        state = State.GAME_OVER;
        againAnswers.clear();
        broadcast(prompt(Protocol.P_AGAIN));
    }

    private void handleAgain(ClientEndpoint s, String answer) {
        if (state != State.GAME_OVER) return;
        againAnswers.put(s, answer.toLowerCase().startsWith("y"));
        if (againAnswers.size() < 2) {
            s.send(msg("Waiting for the other player..."));
            return;
        }
        boolean bothYes = true;
        for (boolean v : againAnswers.values()) bothYes = bothYes && v;
        if (bothYes) startGame();
        else endSession("A player chose not to continue.");
    }

    // ---- disconnect / end ------------------------------------------------

    public synchronized void onDisconnect(ClientEndpoint s) {
        if (!players.contains(s)) return;
        String joinedName = playerName(s);
        String who = joinedName != null ? joinedName : "A player";

        if (state == State.IN_GAME) {
            if (game != null) game.abort();
            ClientEndpoint opp = other(s);
            if (opp != null) {
                PlayerInfo oppInfo = infoFor(opp);
                if (oppInfo != null) score.recordWin(oppInfo.name());
                opp.send(msg("Opponent left. You win by forfeit."));
                opp.send(msg(score.standings()));
                opp.send(bye("Session ended."));
                opp.close();
            }
        } else {
            for (ClientEndpoint o : players)
                if (o != s) { o.send(msg(who + " left.")); o.send(bye("Session ended.")); o.close(); }
        }
        s.close();
        resetRoom();
    }

    private void endSession(String reason) {
        broadcast(msg(reason));
        broadcast(bye("Session ended. Final " + score.standings()));
        for (ClientEndpoint s : players) s.close();
        resetRoom();
    }

    private void resetRoom() {
        players.clear();
        names.clear();
        againAnswers.clear();
        info1 = info2 = null;
        game = null;
        score.reset();
        state = State.WAITING;
        System.out.println("[room] reset - waiting for a new pair.");
    }

    // ---- helpers ---------------------------------------------------------

    private boolean bothJoined() {
        return players.size() == 2 && names.containsKey(players.get(0)) && names.containsKey(players.get(1));
    }

    private boolean nameTaken(String name) {
        for (String joinedName : names.values())
            if (name.equalsIgnoreCase(joinedName)) return true;
        return false;
    }

    private PlayerInfo infoFor(ClientEndpoint s) {
        int seat = players.indexOf(s);
        if (seat == 0) return info1;
        if (seat == 1) return info2;
        return null;
    }

    private ClientEndpoint endpointFor(PlayerInfo info) {
        if (info == info1) return players.get(0);
        if (info == info2) return players.get(1);
        return null;
    }

    private String playerName(ClientEndpoint endpoint) { return names.get(endpoint); }

    private PlayerInfo xInfo() { return info1.symbol() == Mark.X ? info1 : info2; }
    private PlayerInfo oInfo() { return info1.symbol() == Mark.X ? info2 : info1; }

    private ClientEndpoint other(ClientEndpoint s) {
        for (ClientEndpoint o : players) if (o != s) return o;
        return null;
    }

    private void broadcast(String line) { for (ClientEndpoint s : players) s.send(line); }

    private String moveErrorMessage(MoveError error) {
        switch (error) {
            case NOT_YOUR_TURN: return "It is not your turn.";
            case OUT_OF_RANGE: return "Row and column must be between 1 and 3.";
            case CELL_OCCUPIED: return "That cell is already taken.";
            default: throw new IllegalArgumentException("Unknown move error: " + error);
        }
    }

    private String msg(String t)    { return Protocol.MSG + " " + t; }
    private String prompt(String k) { return Protocol.PROMPT + " " + k; }
    private String bye(String t)    { return Protocol.BYE + " " + t; }
}
