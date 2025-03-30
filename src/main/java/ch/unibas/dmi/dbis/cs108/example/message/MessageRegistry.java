package ch.unibas.dmi.dbis.cs108.example.message;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scans for all classes that implement {@link MessageHandler} in the
 * <code>ch.unibas.dmi.dbis.cs108.example.message.messagehandlers</code> package
 * and registers them based on the class name (e.g., <code>ChatMessageHandler</code>
 * becomes message type <code>CHAT</code>).
 */
public class MessageRegistry {
    private final ConcurrentHashMap<String, MessageHandler> handlerMap = new ConcurrentHashMap<>();

    /**
     * Initializes all message handlers using Reflections to find classes ending with
     * "MessageHandler" that implement {@link MessageHandler}.
     */
    public void initMessageHandlers() {
        // Scan the package/sub-packages containing your MessageHandler implementations
        Reflections reflections = new Reflections(
                "ch.unibas.dmi.dbis.cs108.example.message.messagehandlers",
                Scanners.SubTypes.filterResultsBy(s -> true)
        );

        // Find all classes that implement MessageHandler
        Set<Class<? extends MessageHandler>> subTypes = reflections.getSubTypesOf(MessageHandler.class);

        for (Class<? extends MessageHandler> clazz : subTypes) {
            // We only want concrete classes named something like XxxMessageHandler
            if (!clazz.isInterface()
                    && !Modifier.isAbstract(clazz.getModifiers())
                    && clazz.getSimpleName().endsWith("MessageHandler")) {

                // e.g. "ChatMessageHandler" -> "CHAT"
                String simpleName = clazz.getSimpleName();
                String messageType = simpleName
                        .substring(0, simpleName.length() - "MessageHandler".length())
                        .toUpperCase();

                try {
                    // Instantiate the handler via no-arg constructor
                    MessageHandler instance = clazz.getDeclaredConstructor().newInstance();
                    handlerMap.put(messageType, instance);
                    System.out.println("Registered message type: " + messageType
                            + " -> " + clazz.getName());
                } catch (Exception e) {
                    System.err.println("Failed to create instance of " + clazz.getName());
                    e.printStackTrace();
                }
            }
        }

        // Print out all registered keys in the map
        System.out.println("All registered message handlers: " + handlerMap.keySet());
    }

    /**
     * Retrieves the {@link MessageHandler} associated with the given message type.
     *
     * @param msgType the message type (e.g. "REQUEST", "ACK", "CHAT")
     * @return the corresponding {@link MessageHandler}, or {@code null} if none exists
     */
    public MessageHandler getHandler(String msgType) {
        return handlerMap.get(msgType);
    }
}
