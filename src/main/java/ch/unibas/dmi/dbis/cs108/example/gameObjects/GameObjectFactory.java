package ch.unibas.dmi.dbis.cs108.example.gameObjects;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code GameObjectFactory} class creates instances of registered {@link GameObject}
 * subclasses via reflection based on a string identifier. It maintains a static registry
 * that maps type names to their corresponding class objects. When a type is requested,
 * the factory looks for a constructor whose parameters match the provided arguments.
 */
public class GameObjectFactory {

    /**
     * A registry mapping string identifiers to their corresponding {@link GameObject} subclasses.
     * The static initializer populates this map with known game object types.
     */
    private static final Map<String, Class<? extends GameObject>> registry = new HashMap<>();

    static {
        registry.put("Player", Player.class);
        registry.put("Square", Square.class);
        registry.put("Ricardo", Ricardo.class);
        registry.put("BandageGuy", BandageGuy.class);
        registry.put("Platform" , Platform.class);
        registry.put("MovingPlatform", MovingPlatform.class);

    }

    /**
     * Creates a {@link GameObject} of the specified {@code type}, using the provided
     * {@code params} as constructor arguments. This method searches all public constructors of
     * the registered class for a signature matching the types of the given parameters.
     * <p>
     * If no matching constructor is found, a {@link RuntimeException} is thrown.
     * If the type is not recognized, an {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param type   A string identifier for the {@link GameObject} subclass.
     * @param params The arguments to be passed to the target constructor.
     * @return A new {@link GameObject} instance of the specified type.
     * @throws IllegalArgumentException if the type is unknown.
     * @throws RuntimeException         if instantiation fails or no matching constructor is found.
     */
    public static GameObject create(String type, Object... params) {
        Class<? extends GameObject> clazz = registry.get(type);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown game object type: " + type);
        }
        try {
            for (Constructor<?> constructor : clazz.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != params.length) {
                    continue;
                }
                boolean match = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (!parameterTypes[i].isInstance(params[i])
                            && !isPrimitiveCompatible(parameterTypes[i], params[i])) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return (GameObject) constructor.newInstance(params);
                }
            }
            throw new RuntimeException("No matching constructor found for type: " + type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + type, e);
        }
    }

    /**
     * Checks whether a primitive parameter type is compatible with the provided argument.
     * For example, if {@code parameterType} is {@code int.class} and {@code arg} is an
     * {@code Integer}, it is considered compatible.
     *
     * @param parameterType The parameter type expected by the constructor.
     * @param arg           The argument to verify compatibility for.
     * @return {@code true} if the {@code arg} is compatible with the primitive type;
     *         {@code false} otherwise.
     */
    private static boolean isPrimitiveCompatible(Class<?> parameterType, Object arg) {
        if (!parameterType.isPrimitive()) {
            return false;
        }
        if (parameterType == int.class && arg instanceof Integer) return true;
        if (parameterType == long.class && arg instanceof Long) return true;
        if (parameterType == float.class && arg instanceof Float) return true;
        if (parameterType == double.class && arg instanceof Double) return true;
        if (parameterType == boolean.class && arg instanceof Boolean) return true;
        if (parameterType == char.class && arg instanceof Character) return true;
        if (parameterType == byte.class && arg instanceof Byte) return true;
        if (parameterType == short.class && arg instanceof Short) return true;
        return false;
    }
}
