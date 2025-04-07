## Think Outside The Room
A jump- and run Platformer for four players, filled with puzzles, which require cooperation and coordination.


## About the Game
The players must complete the room by jumping on platforms and solving puzzles together. -but here is the catch: each character is controlled by two players. 
One player controls movement and throwing, while the other player controls jumping and grabbing. 

## Requirements
- Java 21 
- Platform: PC, Laptop
- Players: 4 players (2 players per character)

## Organisation
Team: WISA 
Members: William Tran, Illia Solohub, Senanur Ates, Aiysha Frutiger

## Launching the Game
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Think Outside The Room – Setup Guide</title>
  <style>
    body {
      font-family: Arial, sans-serif;
      line-height: 1.6;
      background-color: #fafafa;
      margin: 2rem;
    }
    h2 {
      color: #333;
    }
    .box {
      background-color: #f0f0f0;
      border-left: 4px solid #888;
      padding: 1em;
      margin-bottom: 2em;
      border-radius: 5px;
    }
    code {
      background-color: #eee;
      padding: 2px 4px;
      border-radius: 4px;
      font-family: monospace;
    }
    pre {
      background-color: #222;
      color: #eee;
      padding: 10px;
      border-radius: 5px;
      overflow-x: auto;
    }
  </style>
</head>
<body>

<h2> Build the Project</h2>
  <div class="box">
    <pre><code>./gradlew build</code></pre>
    <p>This command compiles your project and creates a runnable JAR:</p>
    <p><code>./build/libs/Think_Outside_The_Room-0.0.1-ALPHA.jar</code></p>
  </div>

<h2> Run the Server</h2>
  <div class="box">
    <pre><code>java -jar ./build/libs/Think_Outside_The_Room-0.0.1-ALPHA.jar server 8888</code></pre>
    <p><em>Note:</em> You can replace <code>8888</code> with any available port number.</p>
  </div>

<h2> Run a Client</h2>
  <div class="box">
    <pre><code>java -jar ./build/libs/Think_Outside_The_Room-0.0.1-ALPHA.jar client 25.36.51.25:9876 [username]</code></pre>
    <p><em>Note:</em> The <code>username</code> is optional. If omitted, the system will generate a default nickname.</p>
  </div>

</body>
</html>

## Documents
[Requirement Analysis](docs/requirementAnalysis.md)

[Game Concept](docs/gameConcept.md)

[QA Concept](docs/QualityAssuranceConcept–ThinkOutsidetheRoom.pdf)

[Network Protocol](docs/NetworkProtocol.pdf)

[Manual](docs/Manual.pdf)

