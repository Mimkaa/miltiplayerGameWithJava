package ch.unibas.dmi.dbis.cs108.example.gameLogicTests;

import ch.unibas.dmi.dbis.cs108.example.BaseTest;
import ch.unibas.dmi.dbis.cs108.example.Main;
import ch.unibas.dmi.dbis.cs108.example.ThinkOutsideTheRoom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An example test class.
 * Checks the output of the {@link Main} class and verifies that it contains "Suggested Nickname:".
 */
public class MainTest extends BaseTest {

    // Streams to capture System.out and System.err output.
    private final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errStream = new ByteArrayOutputStream();

    // Backup for original streams.
    private PrintStream outBackup;
    private PrintStream errBackup;

    // Simulated input: a single newline to trigger use of the suggested nickname.
    private final ByteArrayInputStream testInput = new ByteArrayInputStream("\n".getBytes());

    /**
     * Redirects System.out, System.err, and System.in before each test.
     */
    @BeforeEach
    public void redirectStdOutStdErr() {
        outBackup = System.out;
        errBackup = System.err;
        System.setOut(new PrintStream(outStream));
        System.setErr(new PrintStream(errStream));
        // Redirect System.in to provide simulated user input.
        System.setIn(testInput);
    }

    /**
     * Restores the original System.out and System.err after each test.
     */
    @AfterEach
    public void reestablishStdOutStdErr() {
        System.setOut(outBackup);
        System.setErr(errBackup);
    }

    /**
     * Executes the main method of the Main class and checks that the output contains "Suggested Nickname:".
     */
    @Test
    public void testMain() {
        ThinkOutsideTheRoom.main(new String[]{"client","127.0.0.1:8888"});
        String output = outStream.toString();
        output = removeNewline(output);
        assertTrue(output.contains("Suggested Nickname:"), "Output should contain 'Suggested Nickname:'");
    }

    private static String removeNewline(String str) {
        return str.replace("\n", "").replace("\r", "");
    }
}
