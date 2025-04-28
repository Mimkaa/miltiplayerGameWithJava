// src/test/java/ch/unibas/dmi/dbis/cs108/example/BaseTest.java
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

public abstract class BaseTest {

    @Mock
    Server server;
    @Mock GameSessionManager gsm;
    @Mock Game fakeGame;
    @Mock GameObject fakeObj;
    @Captor ArgumentCaptor<Message> msgCap;

    private static MockedStatic<AsyncManager> asyncMock;

    @BeforeAll
    static void initAsyncManager() {
        asyncMock = Mockito.mockStatic(AsyncManager.class);
        asyncMock
                .when(() -> AsyncManager.run(Mockito.any(Runnable.class)))
                .thenAnswer(inv -> {
                    Runnable r = inv.getArgument(0);
                    r.run();
                    return null;
                });
    }

    @AfterAll
    static void teardownAsyncManager() {
        asyncMock.close();
    }
}
