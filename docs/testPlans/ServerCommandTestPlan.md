---
layout: default
title : Week 6
---

# Test Plan: Server Command Integration

| Version | Project Name     | Author       | Status       | Date       | Comment                                |
|---------|------------------|--------------|--------------|------------|----------------------------------------|
| 0.3     | Server Test Suite | AI Generated | In Progress  | 2025-03-27 | Covers all server command integrations |

---

# Server Command Integration Tests

This document outlines integration tests for server-side commands used in the multiplayer game project. Each command is tested for expected behavior, inputs, and outputs using a UDP-based client-server setup.

Test cases are denoted using the identifier format `/SCTxx/`, where `SCT` stands for **Server Command Test**.

Each command is formatted using a standardized message protocol:

```
COMMAND {OPTION}[<parameters>]||
```

- **{REQUEST}** = client-initiated message
- **{RESPONSE}** = server reply
- Parameters use typed prefixes, e.g., `S:` for strings, `F:` for floats

The server is expected to initialize a default game instance:
```
myGameInstance = new Game("DefaultSessionID", "DefaultGameName");
myGameInstance.startPlayersCommandProcessingLoop();
```

---

# 1. Server Command Test Cases

### /SCT01/ - CREATE Command
**Goal:** Create a new game object (e.g., Player).

**Request:**
```
CREATE {REQUEST}[S:Player, S:MyPlayer, F:100.0, F:200.0, F:25.0, S:MyGameSession]||
```
**Expected Response:**
```
CREATE {RESPONSE}[S:<UUID>, S:Player, S:MyPlayer, F:100.0, F:200.0, F:25.0, S:MyGameSession]||
```

---

### /SCT02/ - PING Command
**Goal:** Test client-server connectivity.

**Request:**
```
PING {REQUEST}[]||
```
**Expected Response:**
```
PONG {RESPONSE}[]||
```

---

### /SCT03/ - GETOBJECTID Command
**Goal:** Retrieve the UUID of a game object by name.

**Request:**
```
GETOBJECTID {REQUEST}[S:MyPlayer]||
```
**Expected Response:**
```
GETOBJECTID {RESPONSE}[S:<objectUuid>]||
```

---

### /SCT04/ - CHANGENAME Command
**Goal:** Rename an existing game object.

**Request:**
```
CHANGENAME {REQUEST}[S:OldName, S:NewName]||
```
**Expected Response:**
```
CHANGENAME {RESPONSE}[S:<objectUuid>, S:NewName]||
```

---

### /SCT05/ - USERJOINED Command
**Goal:** Register a new user and check for nickname conflicts.

**Request:**
```
USERJOINED {REQUEST}[S:MyNickname]||
```
**Expected Response:**
- No conflict: acknowledgement only
- On conflict:
```
USERJOINED {RESPONSE}[S:<suggestedNickname>]||
```

---

### /SCT06/ - LOGOUT Command
**Goal:** Log out a user and clean up server-side references.

**Request:**
```
LOGOUT {REQUEST}[S:MyNickname]||
```
**Expected Response:**
```
LOGOUT {RESPONSE}[S:MyNickname]||
```

---

### /SCT07/ - DELETE Command
**Goal:** Delete a game object from the server.

**Request:**
```
DELETE {REQUEST}[S:MyNickname]||
```
**Expected Response:**
```
DELETE {RESPONSE}[S:MyNickname]||
```

---

### /SCT08/ - EXIT Command
**Goal:** Fully terminate client session.

**Request:**
```
EXIT {REQUEST}[S:MyNickname]||
```
**Expected Response:**
```
EXIT {RESPONSE}[S:MyNickname]||
```

---

### /SCT09/ - LOGIN Command
**Goal:** Look up the UUID of a known player.

**Request:**
```
LOGIN {REQUEST}[S:MyPlayerName]||
```
**Expected Response:**
```
LOGIN {RESPONSE}[S:<objectUuid>]||
```

---

### /SCT10/ - CREATEGAME Command
**Goal:** Create a new game session.

**Request:**
```
CREATEGAME {REQUEST}[S:MyNewGameName]||
```
**Expected Response:**
```
CREATEGAME {RESPONSE}[S:<gameUuid>, S:MyNewGameName]||
```

---

# 2. Hardware and Software Requirements

- Java 21
- Custom reliable UDP networking
- Open UDP port 9876
- Compatible client software (GUI or CLI)

---

# 3. Special Test Scenarios

### Nickname Conflict in USERJOINED
**Condition:** Reuse of an existing nickname  
**Expected:**
```
USERJOINED {RESPONSE}[S:<newSuggestedNickname>]||
```

### Duplicate Name in CHANGENAME
**Condition:** Desired name already taken  
**Expected:** Server responds with modified unique name

### Command Broadcast Validation
**Goal:** Verify that commands such as CREATE, DELETE are broadcast to all clients

---

# 4. Validation Checklist

- [ ] `myGameInstance` is initialized before tests
- [ ] All commands are registered in `CommandRegistry`
- [ ] Message encoding/decoding matches server expectations
- [ ] ACK messages are properly processed
- [ ] Logs confirm handler routing and normalization
- [ ] Multi-client tests confirm broadcast consistency

---

