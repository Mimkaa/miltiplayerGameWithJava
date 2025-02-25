# Escape Pursuit: A Cooperative Multiplayer Escape Room Platformer

## Introduction

Escape Pursuit is a 2D pixel art multiplayer platformer that combines classic escape room puzzles with a high-intensity chase. In this game, only one level exists—a single room that the Escaper must escape from. Two characters share the spotlight on screen: one Hunter and one Escaper. Uniquely, the Escaper is controlled cooperatively by three players, each responsible for one of the core abilities. This design creates an engaging asymmetrical gameplay experience where coordination is the key to success.

## Game Theme

Set within a confined, puzzle-laden room, players are thrust into a race against time:
- **The Hunter:** Tasked with catching the Escaper before they escape.
- **The Escaper:** Must navigate the single room filled with obstacles and puzzles to reach the exit.

The narrative is minimal, focusing on fast-paced action and strategic teamwork rather than deep storytelling. The environment is designed to evoke the retro charm of classic platformers, paired with modern multiplayer collaboration.

## Gameplay Mechanics

### Characters and Roles

**Escaper:**
- **Control:** A single character that is controlled collaboratively by three players.
- **Player Responsibilities:**
    - **Walking:** One player is dedicated to steering the Escaper.
    - **Jumping:** A second player handles the jump commands.
    - **Grabbing:** A third player manages the grabbing or interaction actions.
- **Objective:** Work together to navigate through the obstacles in the room and reach the exit.
- **Cooperation:** The three players must coordinate their inputs precisely to successfully guide the Escaper to safety.

### Level Design and Objectives

- **Single Level:** The game consists of one level—a single room that the Escaper must escape.
- **Dynamic Obstacles:** The room is designed like a platformer stage with moving platforms, traps, and puzzles that challenge the coordinated actions of the Escaper team.
- **Exit Goal:** The Escaper must reach the designated exit point to win, while the Hunter aims to intercept and catch the Escaper.
- **Challenge and Balance:** The room’s layout is carefully balanced to reward teamwork and precision while maintaining constant tension between pursuit and evasion.

## Networking and Architecture

Escape Pursuit utilizes a client/server architecture with a custom text-based network protocol:

- **Server Responsibilities:**
    - Manage the global game state, including player positions, collisions, and interactions within the single room.
    - Handle the game session and process commands (e.g., MOVE, JUMP, HOLD) using a custom, easily debuggable protocol.
    - Enforce game rules such as win/lose conditions—determining if the Escaper escapes or is caught by the Hunter.

- **Client Responsibilities:**
    - Provide a graphical user interface (GUI) for capturing player inputs and displaying the game state.
    - Send user commands to the server and receive real-time updates to maintain synchronization across all players.
    - Include chat and lobby functionality for pre-game organization and communication during gameplay.

- **Network Protocol:**
    - Communication is based on a self-designed, text-based protocol that supports clear, debuggable exchanges between the server and clients.

## Unique Selling Points

- **Asymmetrical Control:** The Escaper is controlled by three players working in concert, fostering deep cooperative gameplay.
- **High-Stakes Chase:** With a delayed start and slower movement, the Hunter’s pursuit adds constant tension to the single-room escape challenge.
- **Dynamic Platforming in a Single Room:** Despite being confined to one room, the level is packed with diverse obstacles and puzzles that demand precise coordination.
- **Retro Aesthetics:** Charming 2D pixel art visuals deliver nostalgic appeal combined with modern multiplayer features.
- **Robust Networking:** The custom client/server architecture ensures smooth, synchronized gameplay and effective communication.

## Development Milestones and Achievements

Escape Pursuit is designed to meet several key milestones:
- **About a Game & Advanced Concept:** A clear, well-documented game concept (within two pages) that details the game’s objectives, rules, and mechanics.
- **Networking Overview:** A detailed explanation of client and server responsibilities, along with a custom protocol.
- **Requirement Analysis:** Comprehensive documentation of software requirements.
- **Project Diary (Dear Diary):** Ongoing project documentation that includes meaningful entries.
- **Mockup Presentation:** A self-made visual mockup of the game to be shown during presentations.
- **Project Timeline and Plan (Who? What? When?):** A detailed project plan outlining team responsibilities and deadlines.
- **Early Code and Testing:** Initial runnable code and networking tests will be implemented ahead of schedule to demonstrate proof of concept.

## Conclusion

Escape Pursuit offers a unique blend of competitive chase and cooperative platforming within a single, challenging room. With one Hunter and one Escaper—collaboratively controlled by three players—set in a dynamically designed room filled with obstacles, the game emphasizes teamwork, precise coordination, and strategic gameplay. Designed with a robust client/server architecture and a custom communication protocol, Escape Pursuit meets both the creative and technical requirements of the cs108 programming project while delivering an engaging, memorable gaming experience.

Let's embark on this adventure where strategy, skill, and cooperation are the keys to escaping the relentless pursuit!
