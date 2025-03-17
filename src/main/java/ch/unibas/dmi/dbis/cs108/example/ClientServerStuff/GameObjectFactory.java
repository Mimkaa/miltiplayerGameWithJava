package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class GameObjectFactory {
    // A registry mapping type names to the corresponding GameObject subclass.
    private static final Map<String, Class<? extends GameObject>> registry = new HashMap<>();

    static {
        // Register your game object classes here.
        registry.put("Player", Player.class);
        registry.put("Square", Square.class);
        registry.put("Ricardo", Ricardo.class);
        registry.put("BandageGuy", BandageGuy.class);
    }

    /**
     * Creates a GameObject of the given type using the provided parameters.
     *
     * @param type   The string identifier for the type of game object.
     * @param params An array of parameters that will be passed to the constructor.
     * @return A new instance of the specified game object.
     * @throws IllegalArgumentException if the type is unknown or no matching constructor is found.
     */
    public static GameObject create(String type, Object... params) {
        Class<? extends GameObject> clazz = registry.get(type);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown game object type: " + type);
        }
        try {
            // Iterate through all public constructors of the class.
            for (Constructor<?> constructor : clazz.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != params.length) {
                    continue;
                }
                boolean match = true;
                // Check if each parameter is compatible with the constructor's parameter types.
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (!parameterTypes[i].isInstance(params[i]) &&
                        !isPrimitiveCompatible(parameterTypes[i], params[i])) {
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
     * Helper method to check compatibility when a parameter type is primitive.
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
