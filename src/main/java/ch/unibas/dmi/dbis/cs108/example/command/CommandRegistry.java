package ch.unibas.dmi.dbis.cs108.example.command;

import lombok.Getter;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code CommandRegistry} class uses reflection to automatically discover
 * and register classes in the <code>ch.unibas.dmi.dbis.cs108.example.command.commandhandlers</code>
 * package (and its subpackages) that implement the {@link CommandHandler} interface
 * and whose class name ends with <code>"CommandHandler"</code>.
 * <p>
 * For example, a class named <code>PingCommandHandler</code> is registered under the
 * command name <code>PING</code>, because the suffix <code>"CommandHandler"</code> is
 * stripped and the remainder is converted to uppercase.
 * <p>
 * This approach allows for a "zero configuration" pattern:
 * simply adding a new command handler class (e.g., <code>FooCommandHandler</code>)
 * with a public no-argument constructor is enough for it to be discovered
 * and registered at runtime, without manual edits to the code or any explicit
 * enumeration of commands.
 */
@Getter
public class CommandRegistry {

    /**
     * Maps each command name (e.g., "PING") to its corresponding
     * {@link CommandHandler} instance.
     * -- GETTER --
     *  Exposes the internal map of registered commands and their handlers.
     *  Primarily useful for debugging or inspection.
     *
     * @return the concurrent map of command names to their respective handlers

     */
    private final ConcurrentHashMap<String, CommandHandler> commandHandlers = new ConcurrentHashMap<>();

    /**
     * Scans the <code>ch.unibas.dmi.dbis.cs108.example.command.commandhandlers</code>
     * package for all classes that implement {@link CommandHandler}, end with
     * <code>"CommandHandler"</code> in their class name, and are not abstract.
     * <p>
     * For each matching class, this method instantiates it via its no-argument
     * constructor, determines the command name (by removing the <code>"CommandHandler"</code>
     * suffix and converting to uppercase), and stores it in the
     * {@link #commandHandlers} map.
     * <p>
     * Finally, it prints a summary of the registered commands to <code>System.out</code>.
     */
    public void initCommandHandlers() {
        Reflections reflections = new Reflections(
                "ch.unibas.dmi.dbis.cs108.example.command.commandhandlers",
                Scanners.SubTypes.filterResultsBy(s -> true)
        );

        Set<Class<? extends CommandHandler>> handlerClasses = reflections.getSubTypesOf(CommandHandler.class);

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

    /**
     * Retrieves the {@link CommandHandler} associated with a given command name.
     * For instance, if a class named <code>PingCommandHandler</code> was discovered,
     * it will be accessible via <code>getHandler("PING")</code>.
     *
     * @param commandType the uppercase command name (e.g. "PING", "CREATE", etc.)
     * @return the matching {@link CommandHandler} if found, or {@code null} otherwise
     */
    public CommandHandler getHandler(String commandType) {
        return commandHandlers.get(commandType);
    }

}