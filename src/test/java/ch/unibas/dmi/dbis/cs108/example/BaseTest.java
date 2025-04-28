// src/test/java/ch/unibas/dmi/dbis/cs108/example/BaseTest.java
package ch.unibas.dmi.dbis.cs108.example;

import ch.unibas.dmi.dbis.cs108.example.ClientServerStuff.AsyncManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public abstract class BaseTest {

    private static MockedStatic<AsyncManager> asyncMock;

    @BeforeAll
    static void initAsyncManagerMock() {
        // Hier legen wir als Default-Answer fest, was bei jedem statischen Aufruf passieren soll.
        asyncMock = Mockito.mockStatic(AsyncManager.class, invocation -> {
            Method m = invocation.getMethod();
            Object[] args = invocation.getArguments();

            switch (m.getName()) {
                case "run":
                    // Überprüfe, ob Runnable-Overload
                    if (args.length == 1 && args[0] instanceof Runnable) {
                        ((Runnable) args[0]).run();
                        return null;
                    }
                    // oder Callable-Overload
                    if (args.length == 1 && args[0] instanceof Callable) {
                        try {
                            return ((Callable<?>) args[0]).call();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
                case "runLoop":
                    // Einfach einmal ausführen (nicht endlos)
                    if (args.length == 1 && args[0] instanceof Runnable) {
                        ((Runnable) args[0]).run();
                        return null;
                    }
                    break;
                default:
                    break;
            }
            // Für alle anderen statischen Methoden gehen wir zurück zur echten Implementierung
            return invocation.callRealMethod();
        });
    }

    @AfterAll
    static void teardownAsyncManagerMock() {
        asyncMock.close();
    }
}
