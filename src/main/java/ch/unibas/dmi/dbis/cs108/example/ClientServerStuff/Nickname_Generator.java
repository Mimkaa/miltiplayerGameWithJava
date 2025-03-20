package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;

import java.util.Random;

public class Nickname_Generator {

    public static String generateNickname() {
        String username = System.getProperty("user.name");
        String[] adjectives = {"Lilly", "Tina", "Yuki", "Lolo", "Ais", "XoX"};
        Random random = new Random();

        return adjectives[random.nextInt(adjectives.length)] + "_" + username + "_" + (random.nextInt(90) + 10);
    }

}

