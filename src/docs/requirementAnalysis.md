---
layout: default
title : Week 6
---

# Requirements Specification
##### (Based on Lichter & Ludwig, Software Engineering: Fundamentals, People, Processes, Techniques)

## 1. Introduction

### 1.1 Purpose

This document outlines the requirements specification for our multiplayer Escape Room game—a 2D pixel art platformer with asymmetrical, cooperative gameplay. It serves to define the functional and non-functional requirements, scope, and constraints of the project. This specification is intended for developers, project managers, and other stakeholders involved in the cs108 programming project.

### 1.2 Scope and Objectives

The software will be deployed as a multiplayer game featuring exactly two on-screen characters with distinct roles and control schemes:
- **Hunter:** Controlled by one player, the Hunter has the abilities to run, jump, and hold. The Hunter's objective is to catch the Escaper.
- **Escaper:** A single character that is cooperatively controlled by three players. One player is responsible for walking, another for jumping, and the third for executing the grabbing action. The Escaper’s goal is to reach the exit while overcoming a variety of obstacles in a platformer-style level.

**Game Concept Details:**
- **Theme & Visual Style:** A retro 2D pixel art design with an emphasis on clear, playful graphics.
- **Gameplay Mechanics:**
    - The level is designed as a dynamic platformer filled with obstacles (moving platforms, traps, puzzles) that challenge the coordinated actions of the Escaper team.
    - The Hunter starts with a time delay and moves slower than the Escaper, balancing the inherent difficulty of cooperative control.
    - Game rules specify that if the Hunter touches the Escaper, the game is lost; if the Escaper reaches the exit, the team wins.
- **Networking and Architecture:**
    - The game uses a client/server architecture with a custom, text-based network protocol.
    - The server handles game state management (collision detection, score tracking, session management) while clients capture player inputs and display the synchronized game state.

This clear and well-thought-out game concept (outlined in less than two pages) satisfies the milestone achievements for “About a Game” and “About a Game (advanced).”

### 1.3 Definitions

- **Hunter:** The character, controlled by a single player, whose objective is to catch the Escaper.
- **Escaper:** The single character that must escape; controlled collaboratively by three players responsible for walking, jumping, and grabbing respectively.
- **Client/Server Architecture:** A design in which the server maintains the central game state and logic while clients provide the user interface and send player commands.
- **Network Protocol:** A custom, text-based set of rules for communication between the server and clients.
- **GUI (Graphical User Interface):** The visual layer that allows players to interact with the game and view the current state.

### 1.4 Referenced Documents

- cs108 Programming Project Requirements and Lecture Materials ([https://p9.dmi.unibas.ch/cs108/2025/](https://p9.dmi.unibas.ch/cs108/2025/))
- Lecture slides on Project Management, Client/Server Architectures, Organization, and Project Overview.
- Lichter & Ludwig, *Software Engineering: Fundamentals, People, Processes, Techniques*.

### 1.5 Overview

This specification document is organized as follows:
- **Section 2: General Description** – Describes how the system integrates into its environment, outlines major functions, user profiles, constraints, and assumptions.
- **Section 3: Detailed Requirements** – Enumerates each functional requirement with traceable identifiers.
- **Section 4: Acceptance Criteria** – Specifies how to verify that the requirements have been met.
- **Annex A: Use Cases** – Provides detailed interaction scenarios and workflows.

Additionally, the game concept includes:
- A self-made mockup for presentation.
- A clear networking design outlining client and server responsibilities.
- A project plan detailing timeline and responsibilities (Who? What? When?) along with a project diary (Dear Diary) to document progress.

## 2. General Description

### 2.1 System Integration

The game will operate on a distributed client/server architecture:
- **Server:** Manages the global game state (player positions, collisions, game rules, score tracking), handles multiple game sessions, and processes commands using a custom text-based protocol.
- **Clients:** Connect to the server to send player commands (e.g., MOVE, JUMP, HOLD) and receive real-time updates. The client application includes a GUI for both gameplay and chat functionality.

### 2.2 Functions

The primary functions of the system are:
- **Real-Time Multiplayer Gameplay:** Synchronizing the game state among the Hunter and the cooperatively controlled Escaper.
- **Network Communication:** Facilitating robust data exchange between clients and the server using a custom protocol.
- **Graphical User Interface:** Displaying the game environment, player statuses, and chat features.
- **Lobby and Chat Functionality:** Allowing players to communicate, form teams, and select game sessions.
- **Role-Specific Actions:**
    - The **Hunter** executes running, jumping, and holding actions (single player control).
    - The **Escaper** character is controlled by three players, each handling walking, jumping, or grabbing actions.
- **Obstacle and Level Management:** The server oversees the platformer environment, including dynamic obstacles and exit detection.

### 2.3 User Profiles

- **Hunter Player:** A user skilled in strategic gameplay and timing, responsible for controlling the Hunter.
- **Escaper Players:** A team of three users who must coordinate to control the single Escaper character. Effective communication and teamwork are essential.
- **General Gamers:** Players with basic computer proficiency and familiarity with multiplayer games, with an interest in cooperative gameplay.

### 2.4 Constraints

- **Platform:** The game must be developed in Java (version 21) and run on standard desktop systems.
- **Network Protocol:** Must use a custom, text-based protocol to ensure clarity and ease of debugging.
- **Hardware:** Designed to operate on typical student hardware without specialized equipment.
- **Content:** Must adhere to non-violence guidelines and be appropriate for all audiences.
- **Approved Libraries:** Only libraries on the official "Allow List" may be used.

### 2.5 Assumptions and Dependencies

- Players will have stable internet connectivity.
- The development environment supports Java 21 and required external libraries.
- The game will be deployed and tested within a controlled university network environment.
- External dependencies include approved libraries for GUI development and network communication.

## 3. Detailed Requirements

Each functional requirement is assigned a unique identifier for traceability:

* /F10/ **Client-Server Connection:**  
  The system shall allow multiple clients to connect to the central server using a custom text-based protocol.

* /F11/ **User Authentication and Lobby Management:**  
  The system shall support user authentication and provide a lobby where players can chat and select game sessions.

* /F20/ **Real-Time Gameplay Synchronization:**  
  The system shall maintain a synchronized game state across all connected clients, including player positions and actions.

* /F21/ **Role-Specific Actions:**  
  The system shall differentiate between the two roles:
    - **Hunter:** Executes running, jumping, and holding actions (controlled by one player).
    - **Escaper:** A single character controlled collaboratively by three players, where one handles walking, one manages jumping, and one controls grabbing.

* /F30/ **Game State Management:**  
  The server shall manage all aspects of the game state, including collision detection, obstacle interactions, score tracking, and win/lose conditions based on:
    - **Hunter Objective:** Catch the Escaper.
    - **Escaper Objective:** Reach the exit while avoiding capture.

* /F40/ **Chat and Communication:**  
  The system shall provide chat functionality in both the lobby and in-game sessions to facilitate communication among players.

## 4. Acceptance Criteria

The following criteria must be met to verify the successful implementation of the requirements:

* /A10/ **Client-Server Communication Test:**  
  The system must successfully establish and maintain a connection between at least four clients (one Hunter and three Escaper controllers) and the server, with proper command exchange via the custom protocol.

* /A20/ **Gameplay Functionality Verification:**  
  The system must demonstrate complete and synchronized gameplay across all clients, including role-specific actions for the Hunter and the cooperatively controlled Escaper. The game must correctly evaluate win/lose conditions based on whether the Hunter catches the Escaper or the Escaper reaches the exit.

# Annex

## Annex A. Use Cases

### Use Case 1: Establishing Connection and Joining a Game
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

#### Exception Flow 1a: Invalid Credentials
* **Flow:**
    1. The client submits incorrect login details.
    2. The server rejects the login.
    3. The client is prompted to re-enter valid credentials.

### Use Case 2: Role-Based Gameplay Action
* **Name:** Execute Role-Specific Action
* **Actors:** Hunter Player, Escaper Players, Server
* **Preconditions:**
    - The client is connected and the game session has started.
* **Standard Flow:**
    1. A player sends an action command (e.g., MOVE, JUMP, HOLD) to the server.
    2. The server validates and processes the command.
    3. The server updates the game state and broadcasts the change to all clients.
* **Postconditions (Success):**
    - The game state accurately reflects the executed action.
* **Postconditions (Failure):**
    - If the command is invalid, the server sends an error message to the client.

#### Exception Flow 2a: Command Timeout
* **Flow:**
    1. A client command is not acknowledged within the expected time frame.
    2. The client re-sends the command.
    3. The server processes the command if it is valid.
