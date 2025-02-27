---
layout: default
title : Week 6 - Requirements Analysis & Game Concept
---

# Requirements Specification and Game Concept: Think Outside the Room

##### (Based on Lichter & Ludwig, Software Engineering: Fundamentals, People, Processes, Techniques)

This document merges the detailed game concept of **Think Outside the Room** with its requirements analysis.
Think Outside the Room is a cooperative multiplayer escape room platformer set in a single, intricately designed room. 
The level is filled with dynamic obstacles and puzzles, requiring precise coordination between players. 
Two characters share the spotlight on screen: two Escapers. Each Escaper is controlled collaboratively by two players, with each managing distinct actions.

## 1. Introduction

### 1.1 Purpose

This document outlines both the game concept and the detailed requirements for Think Outside the Room 
It provides a comprehensive analysis of the software’s scope, functions, and constraints while presenting a clear, 
engaging game concept for all stakeholders, including developers and project managers in the cs108 programming project.

### 1.2 Scope and Objectives

**Think Outside the Room** is a 2D pixel art multiplayer game set in one complex room that two Escapers must escape. 
The game utilizes a dynamic camera system to follow character movements.

- **Escapers:** Two characters, each controlled collaboratively by two players. 
- One player is responsible for walking and grabbing, while the other handles jumping and throwing. 
- The camera smoothly follows each Escaper, ensuring visibility of key obstacles and the exit.

The core objectives are:
- **Escaper Objective:** Navigate through the room’s obstacles, obtain a key, and reach the exit.
- **Technical Objectives:** Utilize a client/server architecture with a custom text-based network protocol for real-time gameplay synchronization.

### 1.3 Definitions

- **Escapers:** Two characters controlled cooperatively by two players each. One player controls walking and grabbing, while the other handles jumping and throwing.
- **Client/Server Architecture:** A system design where the server manages the central game state and logic while the clients handle user interactions and display.
- **Network Protocol:** A custom, text-based protocol for communication between the server and clients.
- **GUI (Graphical User Interface):** The visual interface through which players interact with the game.

## 2. General Description

### 2.1 System Integration

Think Outside the Room operates on a distributed client/server architecture:
- **Server:** Manages the global game state—including player positions, collision detection, obstacle interactions, 
and win/lose conditions—and processes commands using a custom text-based protocol.
- **Clients:** Provide a GUI for capturing player inputs and displaying the game state. 
They also manage the dynamic camera system, ensuring that the part of the room that is not immediately visible is gradually revealed as the characters move.

### 2.2 Functions

Key functions of the system include:
- **Real-Time Multiplayer Gameplay:** Synchronizes the game state among the four controllers of the two Escapers.
- **Network Communication:** Facilitates robust data exchange between clients and the server via a custom protocol.
- **Graphical User Interface:** Displays the game environment and supports chat functions.
- **Dynamic Camera Systems:**
  - Each **Escaper’s camera** continuously follows the Escaper, smoothly scrolling to reveal new parts of the room.
- **Lobby and Chat:** Enables players to communicate, form teams, and select game sessions.
- **Role-Specific Actions:**
  - **Escapers:** Two characters controlled by two players each, with designated roles for walking/grabbing and jumping/throwing.
- **Obstacle and Level Management:** The single room is designed as a platformer with dynamic obstacles, traps, and puzzles.

### 2.3 User Profiles

- **Escaper Players:** A team of four users who must work in unison to control two Escaper characters. 
Their success depends on tight coordination and effective communication.
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

* /F10/ **Client-Server Connection:** The system shall allow multiple clients to connect to the central server using a custom text-based protocol.
* /F11/ **User Authentication and Lobby Management:** The system shall support user authentication and provide a lobby for 
players to chat and select game sessions.
* /F20/ **Real-Time Gameplay Synchronization:** The system shall maintain a synchronized game state across all clients.
* /F21/ **Role-Specific Actions:** The system shall differentiate between:
**Escapers:** Two characters controlled by two players each, with designated roles for walking/grabbing and jumping/throwing.
* /F22/ **Dynamic Camera Systems:** The system shall implement camera functionality for both Escapers.
* /F30/ **Game State Management:** The server shall manage the overall game state, including collision detection and win/lose conditions.
* /F40/ **Chat and Communication:** The system shall provide chat functionality in both the lobby and in-game sessions.

## 4. Acceptance Criteria

The following criteria must be met:
* /A10/ **Client-Server Communication Test:** The system must successfully establish and maintain connections between at least four clients and the server.
* /A20/ **Gameplay Functionality Verification:** The system must demonstrate complete and synchronized gameplay, 
including role-specific actions and dynamic camera behavior.

