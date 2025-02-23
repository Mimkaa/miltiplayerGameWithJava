---
layout: default
title : Week 6 - Requirements Analysis & Game Concept
---

# Requirements Specification and Game Concept: Escape Pursuit

##### (Based on Lichter & Ludwig, Software Engineering: Fundamentals, People, Processes, Techniques)

This document merges the detailed game concept of **Escape Pursuit** with its requirements analysis. Escape Pursuit is a cooperative multiplayer escape room platformer set in a single, intricately designed room. The level is larger than the visible screen area, requiring dynamic camera systems. Two characters share the spotlight on screen: the Hunter and the Escaper. Uniquely, the Escaper is controlled by three players working together, with each handling one specific ability.

## 1. Introduction

### 1.1 Purpose

This document serves to outline both the game concept and the detailed requirements for Escape Pursuit. It provides a comprehensive analysis of the software’s scope, functions, and constraints while presenting a clear, engaging game concept for all stakeholders, including developers and project managers in the cs108 programming project.

### 1.2 Scope and Objectives

**Escape Pursuit** is a 2D pixel art multiplayer game set in one complex room that the Escaper must escape. The room is larger than the display, so both characters benefit from dynamic, following camera systems:
- **Hunter:** Controlled by one player, the Hunter has a camera that follows their movement, though with a restricted field of view to increase the challenge.
- **Escaper:** A single character controlled collaboratively by three players, with one player responsible for walking, another for jumping, and a third for grabbing. The Escaper’s camera smoothly follows the character, ensuring visibility of key obstacles and the exit.

The core objectives are:
- **Hunter Objective:** Catch the Escaper.
- **Escaper Objective:** Navigate through the room’s obstacles to reach the exit.
- **Technical Objectives:** Utilize a client/server architecture with a custom text-based network protocol for real-time gameplay synchronization.

### 1.3 Definitions

- **Hunter:** The character pursued by the Escaper, controlled by one player. Equipped with running, jumping, and holding abilities. The Hunter’s camera has a restricted view to simulate limited visibility.
- **Escaper:** The character that must escape; controlled cooperatively by three players. Each player manages one of the following abilities: walking, jumping, or grabbing. The Escaper’s camera follows the character to reveal new areas as they move.
- **Client/Server Architecture:** A system design where the server manages the central game state and logic while the clients handle user interactions and display.
- **Network Protocol:** A custom, text-based protocol for communication between the server and clients.
- **GUI (Graphical User Interface):** The visual interface through which players interact with the game.

### 1.4 Referenced Documents

- cs108 Programming Project Requirements and Lecture Materials ([https://p9.dmi.unibas.ch/cs108/2025/](https://p9.dmi.unibas.ch/cs108/2025/))
- Lecture slides on Project Management, Client/Server Architectures, Organization, and Project Overview.
- Lichter & Ludwig, *Software Engineering: Fundamentals, People, Processes, Techniques*.

### 1.5 Overview

This document is organized as follows:
- **Section 2: General Description** – Outlines system integration, key functions, user profiles, constraints, and assumptions.
- **Section 3: Detailed Requirements** – Lists all functional requirements with traceable identifiers.
- **Section 4: Acceptance Criteria** – Defines criteria for verifying that the system meets the requirements.
- **Annex A: Use Cases** – Provides detailed use cases illustrating key interactions within the system.

Additionally, the concept supports several project milestones:
- A clear game concept (About a Game, Advanced Concept).
- Networking and requirement analysis.
- Self-made mockups and an initial project diary.
- A detailed project timeline and early runnable code.

## 2. General Description

### 2.1 System Integration

Escape Pursuit operates on a distributed client/server architecture:
- **Server:** Manages the global game state—including player positions, collision detection, obstacle interactions, and win/lose conditions—and processes commands using a custom text-based protocol.
- **Clients:** Provide a GUI for capturing player inputs and displaying the game state. They also manage the dynamic camera system, ensuring that the part of the room that is not immediately visible is gradually revealed as the characters move.

### 2.2 Functions

Key functions of the system include:
- **Real-Time Multiplayer Gameplay:** Synchronizes the game state among the Hunter and the three controllers of the Escaper.
- **Network Communication:** Facilitates robust data exchange between clients and the server via a custom protocol.
- **Graphical User Interface:** Displays the game environment and supports chat functions.
- **Dynamic Camera Systems:**
  - The **Escaper’s camera** continuously follows the Escaper, smoothly scrolling to reveal new parts of the room.
  - The **Hunter’s camera** also follows the Hunter but with a restricted field of view to add an element of challenge.
- **Lobby and Chat:** Enables players to communicate, form teams, and select game sessions.
- **Role-Specific Actions:**
  - **Hunter:** Executes running, jumping, and holding actions (controlled by one player).
  - **Escaper:** A single character controlled by three players, with designated roles for walking, jumping, and grabbing.
- **Obstacle and Level Management:** The single room is designed as a platformer with dynamic obstacles, traps, and puzzles.

### 2.3 User Profiles

- **Hunter Player:** A user who controls the Hunter, requiring precision and strategic timing. Must manage the challenges of a restricted camera view.
- **Escaper Players:** A team of three users who must work in unison to control the single Escaper character. Their success depends on tight coordination and effective communication.
- **General Gamers:** Players with basic computer proficiency and familiarity with multiplayer cooperative games.

### 2.4 Constraints

- **Platform:** Developed in Java (version 21) for standard desktop systems.
- **Network Protocol:** A custom text-based protocol for clear, debuggable communication.
- **Hardware:** Designed for typical student hardware without specialized requirements.
- **Content:** Must adhere to non-violence guidelines and be appropriate for all ages.
- **Approved Libraries:** Only libraries on the official "Allow List" are permitted.

### 2.5 Assumptions and Dependencies

- All players will have stable network connections.
- The development environment supports Java 21 and necessary external libraries.
- The game will be deployed and tested within a controlled university network.
- Dependencies include approved libraries for GUI development, dynamic camera handling, and network communication.

## 3. Detailed Requirements

Each functional requirement is uniquely identified for traceability:

* /F10/ **Client-Server Connection:**  
  The system shall allow multiple clients to connect to the central server using a custom text-based protocol.

* /F11/ **User Authentication and Lobby Management:**  
  The system shall support user authentication and provide a lobby for players to chat and select game sessions.

* /F20/ **Real-Time Gameplay Synchronization:**  
  The system shall maintain a synchronized game state across all clients, ensuring that all player actions and positions are updated in real time.

* /F21/ **Role-Specific Actions:**  
  The system shall differentiate between:
  - **Hunter:** Executes running, jumping, and holding actions, controlled by one player.
  - **Escaper:** A single character controlled by three players, with designated roles for walking, jumping, and grabbing.

* /F22/ **Dynamic Camera Systems:**  
  The system shall implement camera functionality such that:
  - The **Escaper’s camera** continuously follows the Escaper character, smoothly revealing areas of the room as they move.
  - The **Hunter’s camera** follows the Hunter but with a restricted field of view to simulate limited visibility.
  - The level (single room) is larger than the display area, requiring scrolling and camera movement.

* /F30/ **Game State Management:**  
  The server shall manage the overall game state, including collision detection, obstacle interactions, and evaluation of win/lose conditions:
  - **Hunter Objective:** Catch the Escaper.
  - **Escaper Objective:** Reach the exit of the room.

* /F40/ **Chat and Communication:**  
  The system shall provide chat functionality in both the lobby and in-game sessions to facilitate communication among players.

## 4. Acceptance Criteria

The following criteria must be met to verify that the system satisfies its requirements:

* /A10/ **Client-Server Communication Test:**  
  The system must successfully establish and maintain connections between at least four clients (one Hunter and three Escaper controllers) and the server, with proper command exchanges using the custom protocol.

* /A20/ **Gameplay Functionality Verification:**  
  The system must demonstrate complete and synchronized gameplay, including role-specific actions and dynamic camera behavior. The game must correctly evaluate win/lose conditions based on whether the Hunter catches the Escaper or the Escaper reaches the exit.

## Annex

### Annex A. Use Cases

#### Use Case 1: Establishing Connection and Joining a Game
* **Name:** Connect and Join Game Session
* **Actors:** Client, Server
* **Preconditions:**
  - The client application is running.
  - A stable network connection is available.
* **Standard Flow:**
  1. The client initiates a connection to the server.
  2. The server authenticates the client.
  3. The client enters the lobby and selects a game session.
* **Postconditions (Success):**
  - The client is connected to the selected game session.
* **Postconditions (Failure):**
  - The connection fails and an error message is displayed.

##### Exception Flow 1a: Invalid Credentials
* **Flow:**
  1. The client submits incorrect login details.
  2. The server rejects the login.
  3. The client is prompted to re-enter valid credentials.

#### Use Case 2: Role-Based Gameplay Action
* **Name:** Execute Role-Specific Action
* **Actors:** Hunter Player, Escaper Players, Server
* **Preconditions:**
  - The client is connected and the game session has started.
* **Standard Flow:**
  1. A player sends an action command (e.g., MOVE, JUMP, HOLD) to the server.
  2. The server validates and processes the command.
  3. The server updates the game state and broadcasts the update to all clients.
* **Postconditions (Success):**
  - The game state accurately reflects the executed action.
* **Postconditions (Failure):**
  - If the command is invalid, the server sends an error message to the client.

##### Exception Flow 2a: Command Timeout
* **Flow:**
  1. A client command is not acknowledged within the expected time frame.
  2. The client re-sends the command.
  3. The server processes the command if it is valid.
