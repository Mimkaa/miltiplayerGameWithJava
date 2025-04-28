package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Game;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Message;
import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.Server;
import ch.unibas.dmi.dbis.cs108.example.NotConcurrentStuff.GameSessionManager;
import ch.unibas.dmi.dbis.cs108.example.gameObjects.GameObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.*;

/**
 * A base class for all command‐handler unit tests.
 * <p>
 * Provides:
 * <ul>
 *   <li>Commonly used {@code @Mock} fields: {@link Server}, {@link GameSessionManager},
 *       {@link Game}, {@link GameObject}, and an {@link ArgumentCaptor}{@code <Message>}.</li>
 *   <li>A static {@code MockedStatic<AsyncManager>} that overrides
 *       {@link AsyncManager#run(Runnable)} to execute runnables immediately on the calling thread,
 *       so tests aren’t subject to asynchronous timing issues.</li>
 * </ul>
 * </p>
 */
public abstract class BaseTest {

    @Mock
    Server server;

    @Mock
    GameSessionManager gsm;

    @Mock
    Game fakeGame;

    @Mock
    GameObject fakeObj;

    @Captor
    ArgumentCaptor<Message> msgCap;

    private static MockedStatic<AsyncManager> asyncMock;

    /**
     * Before all tests, mock {@link AsyncManager#run(Runnable)} so that
     * any background tasks are executed synchronously in the same thread.
     */
    @BeforeAll
    static void initAsyncManager() {
        asyncMock = Mockito.mockStatic(AsyncManager.class);
        asyncMock
                .when(() -> AsyncManager.run(Mockito.any(Runnable.class)))
                .thenAnswer(invocation -> {
                    Runnable r = invocation.getArgument(0);
                    r.run();
                    return null;
                });
    }

    /**
     * After all tests have run, close the static mock to restore
     * the original {@code AsyncManager} behavior.
     */
    @AfterAll
    static void teardownAsyncManager() {
        asyncMock.close();
    }
}
