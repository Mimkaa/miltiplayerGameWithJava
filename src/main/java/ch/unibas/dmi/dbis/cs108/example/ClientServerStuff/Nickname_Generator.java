package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.Random;

/**
 * The {@code Nickname_Generator} class provides a utility method to generate a random nickname
 * composed of a random adjective, the local system's username, and a random integer (10–99).
 */
public class Nickname_Generator {

    /**
     * Generates a nickname by combining:
     * <ul>
     *   <li>A randomly chosen adjective from a predefined list.</li>
     *   <li>The current system username, retrieved via {@code System.getProperty("user.name")}.</li>
     *   <li>A random integer between 10 and 99 (inclusive).</li>
     * </ul>
     *
     * <p>The resulting format is:
     * <pre>{@code <adjective>_<system-username>_<two-digit-random-number>}</pre>
     *
     * @return A {@code String} representing the generated nickname.
     */
    public static String generateNickname() {
        String username = System.getProperty("user.name");
        String[] adjectives = {"Lilly", "Tina", "Yuki", "Lolo", "Ais", "XoX"};
        Random random = new Random();

        return adjectives[random.nextInt(adjectives.length)] + "_"
                + username + "_" + (random.nextInt(90) + 10);
    }
}
