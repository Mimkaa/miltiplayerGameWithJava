```md
# Test Plan for GameContext and Client

This document describes the unit tests written for the **GameContext** and **Client** classes. For each test, we explain:

- **What** the test does
- **Why** it is important to test this behavior
- **Potential failure scenarios** (what could go wrong)

---

## 1. GameContext Tests

### 1.1 `testSingletonInstance`
- **What**: Asserts that `GameContext.getInstance()` always returns the same object.
- **Why**: Ensures proper singleton behavior so that global state isn’t split across multiple instances.
- **Failure**: If the constructor or getInstance() logic resets the instance or returns new objects, various parts of the application will operate on inconsistent state.

### 1.2 `testGetCurrentAndSelectedGameIdsInitiallyNull`
- **What**: Verifies that both `currentGameId` and `selectedGameObjectId` are `null` after initialization.
- **Why**: The game must start with no selected session or object; starting values must be predictable.
- **Failure**: If IDs are non-null, UI or logic may show invalid selections or throw NPEs.

### 1.3 `testGetGameByIdReturnsNullIfNotExists`
- **What**: Calls `getGameById()` with a nonexistent ID and expects `null`.
- **Why**: Ensures that lookup only returns actual sessions, preventing unexpected `Game` objects.
- **Failure**: If the method returns a dummy or throws, code relying on `null` to detect absence will break.

### 1.4 `testGetGameByIdAfterAddingSession`
- **What**: Adds a session via `addGameSession()` then verifies `getGameById()` returns a `Game` with correct ID and name.
- **Why**: Validates that the manager correctly stores and retrieves session data.
- **Failure**: If sessions aren’t stored properly, users might not see available games or get wrong details.

### 1.5 `testUpdateWithoutKeyPress`
- **What**: Simulates a frame update when no keys are pressed and confirms `Client.sendMessageBestEffort` is **never** called.
- **Why**: Prevents spurious network traffic and ensures `update()` only sends messages on real input.
- **Failure**: Overwhelming best-effort queue, lag, or unwanted in-game commands could occur.

### 1.6 `testUpdateWithKeyPress`
- **What**: Simulates pressing a key with `selectedGameObjectId` and `currentGameId` set, then verifies exactly one call to `sendMessageBestEffort` with correct concealed parameters.
- **Why**: Confirms that input handling attaches the right context to key events for multiplayer synchronization.
- **Failure**: If parameters are wrong or multiple messages are sent, players will see incorrect movements or desync.

### 1.7 `testDrawWithNoCurrentGame`
- **What**: Invokes the private `draw(GraphicsContext)` with no session selected and verifies zero interactions with `GraphicsContext`.
- **Why**: Prevents unnecessary rendering when no game is active, avoiding blank-screen exceptions or UI glitches.
- **Failure**: Unexpected painting may occur and crash if `Game` is `null`.

### 1.8 `testDrawWithExistingGame`
- **What**: Registers a spied `Game`, sets `currentGameId`, provides a mocked `GraphicsContext` and `Canvas`, then verifies:
  - Background is cleared (`fillRect`) with correct dimensions
  - `Game.draw(gc)` is called exactly once
- **Why**: Ensures our view layering works: clearing the canvas then invoking game-specific rendering.
- **Failure**: If background isn’t cleared, artifacts appear; if `game.draw` isn’t invoked, nothing renders.

---

## 2. Client Tests

### 2.1 `testServerConfiguration`
- **What**: Calls `setServerAddress()` and `setServerPort()`, then asserts static fields are updated.
- **Why**: Allows switching server targets at runtime (e.g., dev vs production) and ensures client uses correct endpoints.
- **Failure**: Misconfiguration would lead to inability to connect to the intended server.

### 2.2 `testSendMessageStaticUpdatesConcealedParams`
- **What**: Sets `username`, calls `sendMessageStatic()` on a message with no concealed params, then checks `concealedParameters` contains the username.
- **Why**: Verifies that outgoing messages always carry the sender’s identity, critical for server-side routing and auth.
- **Failure**: If missing or malformed, server can’t associate messages with users.

### 2.3 `testSendMessageBestEffortSendsUdpPacket`
- **What**: Injects a mocked `DatagramSocket` into the client, calls `sendMessageBestEffort()`, and captures the sent `DatagramPacket` to assert it contains the encoded message payload.
- **Why**: Confirms low-level networking works and that the exact encoded string is sent immediately, bypassing queues.
- **Failure**: Malformed packets or missing calls would break real-time updates (e.g., key press broadcasts).

### 2.4 `testAcknowledgeDelegatesToAckProcessor`
- **What**: Injects a mocked `AckProcessor`, calls `Client.acknowledge()` with a message containing a UUID, and verifies `ackProcessor.addAck()` is invoked with the correct destination address and same UUID.
- **Why**: Ensures ACKs are routed correctly back to the server for reliable delivery semantics.
- **Failure**: Lost ACKs lead to server thinking messages were dropped, causing unnecessary retransmissions or session errors.

### 2.5 `testUpdateLocalClientStateChangeUsername`
- **What**: Invokes private method `updateLocalClientState` with a `CHANGE_USERNAME` message and parameter `"Bob"`.
- **Why**: Verifies that the client's username is updated atomically to the new value.
- **Failure**: If the username isn't updated, subsequent messages may carry the wrong identity.

### 2.6 `testUpdateLocalClientStateFastLogin`
- **What**: Stubs `login()` on a client spy, invokes `updateLocalClientState` with a `FAST_LOGIN` message.
- **Why**: Ensures the FAST_LOGIN trigger correctly calls `login()`.
- **Failure**: Without this, the client won't enter the quick login flow.

### 2.7 `testProcessServerResponseCreateGame`
- **What**: Calls `processServerResponse` with a `CREATEGAME` message and parameters `["idX", "NameX"]`.
- **Why**: Confirms the client's internal `game` field is set to a new `Game("idX","NameX")`.
- **Failure**: If `game` isn't initialized, the client can't render or interact with the new session.

### 2.8 `testProcessServerResponseLogout`
- **What**: Simulates a `LOGOUT` message, stubs `sendMessageStatic`, and checks that a `DELETE` message with parameter `"Bob"` is sent.
- **Why**: Verifies that the logout command is correctly translated into the protocol.
- **Failure**: Without this, the server wouldn't remove the user from the session.

### 2.9 `testGetChatPanelDelegation`
- **What**: Sets `client.clientChatManager` to a mock, stubs its `getChatPanel()` to return a dummy panel, and asserts `client.getChatPanel()` returns it.
- **Why**: Ensures the UI component for chat is correctly delegated by the client.
- **Failure**: Without proper delegation, the chat panel would not display.

---

**End of Test Plan**
```

