package ch.unibas.dmi.dbis.cs108.example.command;

import ch.unibas.dmi.dbis.cs108.example.command.CommandHandler;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Automatically discovers any class in the 'commandhandlers' sub-package
 * that implements CommandHandler and ends with "CommandHandler".
 */
public class CommandRegistry {
    private final ConcurrentHashMap<String, CommandHandler> commandHandlers = new ConcurrentHashMap<>();

    public void initCommandHandlers() {
        // Point Reflections to your sub-package:
        Reflections reflections = new Reflections(
                "ch.unibas.dmi.dbis.cs108.example.command.commandhandlers",
                Scanners.SubTypes.filterResultsBy(s -> true)
        );

        // Find all classes that implement CommandHandler
        Set<Class<? extends CommandHandler>> handlerClasses =
                reflections.getSubTypesOf(CommandHandler.class);

        for (Class<? extends CommandHandler> cls : handlerClasses) {
            // We only want concrete classes named something like XxxCommandHandler
            if (!cls.isInterface()
                    && !Modifier.isAbstract(cls.getModifiers())
                    && cls.getSimpleName().endsWith("CommandHandler")) {

                // e.g. "PingCommandHandler" => "PING"
                String simpleName = cls.getSimpleName();
                String commandName = simpleName
                        .substring(0, simpleName.length() - "CommandHandler".length())
                        .toUpperCase();

                try {
                    // Instantiate the handler via no-arg constructor
                    CommandHandler handler = cls.getDeclaredConstructor().newInstance();
                    commandHandlers.put(commandName, handler);

                    System.out.println("Registered command: " + commandName
                            + " -> " + cls.getName());
                } catch (Exception e) {
                    System.err.println("Failed to create instance of: " + cls.getName());
                    e.printStackTrace();
                }
            }
        }

        System.out.println("All registered commands: " + commandHandlers.keySet());
    }

    public CommandHandler getHandler(String commandType) {
        return commandHandlers.get(commandType);
    }

    public ConcurrentHashMap<String, CommandHandler> getCommandHandlers() {
        return commandHandlers;
    }
}
