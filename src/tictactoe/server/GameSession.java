package tictactoe.server;

import tictactoe.Protocol;
import tictactoe.game.Board;
import tictactoe.game.Mark;
import tictactoe.game.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class GameSession {
    private enum State { WAITING, READY, INVITED, PLAYING, GAME_OVER }

    private final List<PlayerSession> sessions = new ArrayList<>(2);
    private final Map<PlayerSession, Player> players = new HashMap<>();
    private final Map<PlayerSession, Boolean> againAnswers = new HashMap<>();
    private final Board board = new Board();
    private final Random random;

    private State state = State.WAITING;
    private PlayerSession invitedPlayer;
    private Mark turn = Mark.X;
    private int draws;

    public GameSession() {
        this(new Random());
    }

    GameSession(Random random) {
        this.random = random;
    }

    public synchronized boolean register(PlayerSession session) {
        if (sessions.size() >= 2) return false;
        sessions.add(session);
        return true;
    }

    public synchronized void onCommand(PlayerSession session, String line) {
        if (!sessions.contains(session)) return;
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) {
            session.send(message("Command cannot be empty."));
            return;
        }

        String[] parts = trimmed.split("\\s+");
        String command = parts[0].toUpperCase();
        switch (command) {
            case Protocol.JOIN:
                handleJoin(session, parts.length > 1 ? parts[1] : "");
                break;
            case Protocol.INVITE:
                handleInvite(session);
                break;
            case Protocol.ACCEPT:
                handleAccept(session);
                break;
            case Protocol.DECLINE:
                handleDecline(session);
                break;
            case Protocol.MOVE:
                handleMove(session, parts);
                break;
            case Protocol.AGAIN:
                handleAgain(session, parts.length > 1 ? parts[1] : "");
                break;
            case Protocol.QUIT:
                onDisconnect(session);
                break;
            default:
                session.send(message("Unknown command."));
        }
    }

    public synchronized void onDisconnect(PlayerSession session) {
        if (!sessions.contains(session)) return;
        Player leaving = players.get(session);
        PlayerSession remaining = other(session);

        if (state == State.PLAYING && remaining != null) {
            Player winner = players.get(remaining);
            if (winner != null) winner.recordWin();
            remaining.send(message("Opponent left. You win by forfeit."));
            remaining.send(message(standings()));
            remaining.send(bye("Session ended."));
        } else if (remaining != null) {
            String name = leaving == null ? "A player" : leaving.name();
            remaining.send(message(name + " left."));
            remaining.send(bye("Session ended."));
        }

        closeAndReset();
    }

    private void handleJoin(PlayerSession session, String name) {
        if (players.containsKey(session)) {
            session.send(message("You have already joined."));
            return;
        }
        if (name.isEmpty()) {
            session.send(message("Name cannot be empty."));
            session.send(prompt(Protocol.P_NAME));
            return;
        }
        if (isNameTaken(name)) {
            session.send(message("Name already taken, choose another."));
            session.send(prompt(Protocol.P_NAME));
            return;
        }

        players.put(session, new Player(name));
        broadcast(message(name + " joined the room."));
        if (bothPlayersJoined()) {
            state = State.READY;
            PlayerSession first = sessions.get(0);
            PlayerSession second = sessions.get(1);
            first.send(message("Both players are here. You may invite "
                    + player(second).name() + "."));
            first.send(prompt(Protocol.P_INVITE));
            second.send(message("Waiting for " + player(first).name()
                    + " to invite you..."));
        } else {
            session.send(message("Waiting for another player to join..."));
        }
    }

    private void handleInvite(PlayerSession session) {
        if (state != State.READY || !players.containsKey(session)) {
            session.send(message("You cannot invite right now."));
            return;
        }
        invitedPlayer = other(session);
        if (invitedPlayer == null || !players.containsKey(invitedPlayer)) {
            session.send(message("There is no player to invite."));
            return;
        }
        state = State.INVITED;
        invitedPlayer.send(message(player(session).name() + " invited you to play."));
        invitedPlayer.send(prompt(Protocol.P_ACCEPT));
        session.send(message("Invitation sent, waiting for a response..."));
    }

    private void handleAccept(PlayerSession session) {
        if (state != State.INVITED || session != invitedPlayer) {
            session.send(message("Nothing to accept."));
            return;
        }
        broadcast(message(player(session).name() + " accepted. Starting the game."));
        assignInitialMarks();
        startGame();
    }

    private void handleDecline(PlayerSession session) {
        if (state != State.INVITED || session != invitedPlayer) {
            session.send(message("Nothing to decline."));
            return;
        }
        broadcast(message(player(session).name() + " declined the invitation."));
        invitedPlayer = null;
        state = State.READY;
        PlayerSession first = sessions.get(0);
        first.send(prompt(Protocol.P_INVITE));
    }

    private void handleMove(PlayerSession session, String[] parts) {
        if (state != State.PLAYING) {
            session.send(message("No game in progress."));
            return;
        }
        Player mover = player(session);
        if (mover == null) return;
        if (mover.mark() != turn) {
            rejectMove(session, "It is not your turn.");
            return;
        }

        int row;
        int col;
        try {
            row = Integer.parseInt(parts[1]);
            col = Integer.parseInt(parts[2]);
        } catch (RuntimeException error) {
            rejectMove(session, "Enter your move as two numbers: row col.");
            return;
        }

        Board.MoveError moveError = board.place(row, col, mover.mark());
        if (moveError == Board.MoveError.OUT_OF_RANGE) {
            rejectMove(session, "Row and column must be between 1 and 3.");
            return;
        }
        if (moveError == Board.MoveError.CELL_OCCUPIED) {
            rejectMove(session, "That cell is already taken.");
            return;
        }

        broadcast(Protocol.BOARD + " " + board.serialize());
        Board.Outcome outcome = board.outcome();
        if (outcome == Board.Outcome.IN_PROGRESS) {
            turn = turn.opponent();
            promptCurrentTurn();
        } else {
            finishGame(outcome);
        }
    }

    private void handleAgain(PlayerSession session, String answer) {
        if (state != State.GAME_OVER) {
            session.send(message("There is no finished game to continue."));
            return;
        }
        againAnswers.put(session, answer.toLowerCase().startsWith("y"));
        if (againAnswers.size() < 2) {
            session.send(message("Waiting for the other player..."));
            return;
        }
        if (againAnswers.values().stream().allMatch(Boolean::booleanValue)) {
            for (Player player : players.values())
                player.setMark(player.mark().opponent());
            startGame();
        } else {
            endSession("A player chose not to continue.");
        }
    }

    private void assignInitialMarks() {
        Player first = player(sessions.get(0));
        Player second = player(sessions.get(1));
        boolean firstIsX = random.nextBoolean();
        first.setMark(firstIsX ? Mark.X : Mark.O);
        second.setMark(firstIsX ? Mark.O : Mark.X);
    }

    private void startGame() {
        board.reset();
        turn = Mark.X;
        state = State.PLAYING;
        invitedPlayer = null;
        againAnswers.clear();

        Player x = playerWithMark(Mark.X);
        Player o = playerWithMark(Mark.O);
        broadcast(message("New game! " + x.name() + " is X, "
                + o.name() + " is O."));
        sessionFor(x).send(Protocol.SYMBOL + " X");
        sessionFor(o).send(Protocol.SYMBOL + " O");
        broadcast(Protocol.BOARD + " " + board.serialize());
        promptCurrentTurn();
    }

    private void promptCurrentTurn() {
        PlayerSession current = sessionFor(playerWithMark(turn));
        PlayerSession waiting = other(current);
        current.send(prompt(Protocol.P_MOVE));
        if (waiting != null)
            waiting.send(message("Opponent's turn, please wait..."));
    }

    private void finishGame(Board.Outcome outcome) {
        if (outcome == Board.Outcome.DRAW) {
            draws++;
            broadcast(message("It's a draw."));
        } else {
            Mark winningMark = outcome == Board.Outcome.X_WINS ? Mark.X : Mark.O;
            Player winner = playerWithMark(winningMark);
            Player loser = playerWithMark(winningMark.opponent());
            winner.recordWin();
            sessionFor(winner).send(message("You win!"));
            sessionFor(loser).send(message("You lose."));
        }
        broadcast(message(standings()));
        state = State.GAME_OVER;
        againAnswers.clear();
        broadcast(prompt(Protocol.P_AGAIN));
    }

    private void rejectMove(PlayerSession session, String reason) {
        session.send(message(reason));
        session.send(prompt(Protocol.P_MOVE));
    }

    private void endSession(String reason) {
        broadcast(message(reason));
        broadcast(bye("Session ended. Final " + standings()));
        closeAndReset();
    }

    private void closeAndReset() {
        List<PlayerSession> closing = new ArrayList<>(sessions);
        sessions.clear();
        players.clear();
        againAnswers.clear();
        invitedPlayer = null;
        board.reset();
        turn = Mark.X;
        draws = 0;
        state = State.WAITING;
        for (PlayerSession session : closing)
            session.close();
    }

    private boolean bothPlayersJoined() {
        return sessions.size() == 2
                && players.containsKey(sessions.get(0))
                && players.containsKey(sessions.get(1));
    }

    private boolean isNameTaken(String name) {
        for (Player existing : players.values())
            if (existing.name().equalsIgnoreCase(name)) return true;
        return false;
    }

    private Player player(PlayerSession session) {
        return players.get(session);
    }

    private Player playerWithMark(Mark mark) {
        for (Player player : players.values())
            if (player.mark() == mark) return player;
        return null;
    }

    private PlayerSession sessionFor(Player target) {
        for (Map.Entry<PlayerSession, Player> entry : players.entrySet())
            if (entry.getValue() == target) return entry.getKey();
        return null;
    }

    private PlayerSession other(PlayerSession session) {
        for (PlayerSession candidate : sessions)
            if (candidate != session) return candidate;
        return null;
    }

    private String standings() {
        StringBuilder text = new StringBuilder("Score - ");
        for (PlayerSession session : sessions) {
            Player player = players.get(session);
            if (player != null)
                text.append(player.name()).append(": ")
                        .append(player.wins()).append("  ");
        }
        return text.append("(draws: ").append(draws).append(")").toString();
    }

    private void broadcast(String line) {
        for (PlayerSession session : new ArrayList<>(sessions))
            session.send(line);
    }

    private String message(String text) {
        return Protocol.MSG + " " + text;
    }

    private String prompt(String kind) {
        return Protocol.PROMPT + " " + kind;
    }

    private String bye(String text) {
        return Protocol.BYE + " " + text;
    }
}
