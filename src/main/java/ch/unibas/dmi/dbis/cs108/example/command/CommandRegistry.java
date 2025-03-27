package ch.unibas.dmi.dbis.cs108.example.command;

import java.util.concurrent.ConcurrentHashMap;

public class CommandRegistry {
    private final ConcurrentHashMap<String, CommandHandler> commandHandlers = new ConcurrentHashMap<>();

    public void initCommandHandlers() {
        commandHandlers.put("CREATE", new CreateCommandHandler());
        commandHandlers.put("PING", new PingCommandHandler());
        commandHandlers.put("GETOBJECTID", new GetObjectIdCommandHandler());
        commandHandlers.put("CHANGENAME", new ChangeNameCommandHandler());
        commandHandlers.put("USERJOINED", new UserJoinedCommandHandler());
        commandHandlers.put("LOGOUT", new LogoutOrExitCommandHandler("LOGOUT"));
        commandHandlers.put("EXIT", new LogoutOrExitCommandHandler("EXIT"));
        commandHandlers.put("LOGIN", new LoginCommandHandler());
        commandHandlers.put("DELETE", new DeleteCommandHandler());
        commandHandlers.put("CREATEGAME", new CreateGameCommandHandler());

        System.out.println("Server's commandHandlers: " + commandHandlers.keySet());

    }

    public CommandHandler getHandler(String commandType) {
        return commandHandlers.get(commandType);
    }

    public ConcurrentHashMap<String, CommandHandler> getCommandHandlers() {
        return commandHandlers;
    }
}