package ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff;

import java.util.concurrent.ConcurrentHashMap;

public class ResourceManager {
    // A generic map to store any type of resource.
    // The key can be a String that uniquely identifies the resource.
    private final ConcurrentHashMap<String, Object> resources = new ConcurrentHashMap<>();

    // Singleton instance for easy access.
    private static final ResourceManager instance = new ResourceManager();

    private ResourceManager() {
    }

    public static ResourceManager getInstance() {
        return instance;
    }

    /**
     * Adds a resource with the specified key.
     *
     * @param key      A unique identifier for the resource.
     * @param resource The resource object.
     * @param <T>      The type of the resource.
     */
    public <T> void addResource(String key, T resource) {
        resources.put(key, resource);
    }

    /**
     * Retrieves a resource by its key, casting it to the expected type.
     *
     * @param key   The unique identifier for the resource.
     * @param clazz The expected class type of the resource.
     * @param <T>   The type of the resource.
     * @return The resource if present, or null otherwise.
     */
    public <T> T getResource(String key, Class<T> clazz) {
        Object resource = resources.get(key);
        if (resource == null) {
            return null;
        }
        return clazz.cast(resource);
    }

    /**
     * Removes a resource by its key.
     *
     * @param key The unique identifier for the resource to remove.
     */
    public void removeResource(String key) {
        resources.remove(key);
    }
}

