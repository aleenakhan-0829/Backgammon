**Backgammon – Multiplayer Network Game**

**Course:** Computer Network Concepts
**Project:** 2
**University:** Fatih Sultan Mehmet Vakıf University
**Technology:** Java (Swing GUI + TCP Networking)

 **Overview**

This project is a two-player online Backgammon game built in Java.
It uses a client-server architecture where players connect over a network and play in real time.
The server manages all game rules and ensures fairness, while the clients handle the user interface and player interaction.

 **Project Structure**
backgammon/
├── server/
│   ├── BackgammonServer.java   → Starts the server
│   ├── ClientHandler.java      → Handles each connected player
│   ├── GameState.java          → Controls game rules and board logic
│   └── MoveResult.java         → Stores results of moves
│
├── client/
│   ├── Main.java               → Launches the application
│   ├── StartScreen.java        → Connection screen (IP input)
│   ├── NetworkClient.java      → Manages socket connection
│   ├── ClientGameState.java    → Local copy of game state
│   ├── BoardPanel.java         → Draws the board and handles clicks
│   ├── GameWindow.java         → Main game interface
│   └── EndScreen.java          → Shows result and replay option
│
├── compile.bat
├── run_server.sh/run_server.bat
├── run_client.sh/run_client.bat
└── README.md

**System Design**

The system follows a simple client-server model:

Two clients represent the players
One server manages the entire game logic
Communication happens using TCP sockets on port 5555
 Player 1 Client            Player 2 Client
        \                      /
         \   TCP Connection   /
          \                  /
           ---- Server ------
            Game Logic Engine
            
**Server Responsibilities**
Enforces game rules
Validates all moves
Updates board state
Sends updates to both players

 **Client Responsibilities**
Displays game board (Swing GUI)
Sends player actions to server
Receives updates and renders changes

**Communication Protocol**

All messages are sent as simple text lines over TCP.

**Messages**

Client → Server
READY → Player is ready
ROLL → Request dice roll
MOVE:from,to → Move a checker
CHAT:text → Send chat message
RESIGN → Give up
REPLAY → Request new game
Server → Client
PLAYER_NUM:n → Assign player number
START:boarddata → Start game
DICE:d1,d2 → Dice values
BOARD:boarddata → Updated board
YOUR_TURN:roll|move → Your turn
WAIT:reason → Wait for opponent
GAMEOVER:winner → Game finished
CHAT:text → Chat message
ERROR:reason → Invalid move
DISCONNECT:reason → Opponent left

 **Board Data Format**

Board state is sent as:

b0,b1,...,b27|currentPlayer|d1,d2
Meaning:
0–23 → Board positions
24–25 → Bar (captured checkers)
26–27 → Bear-off areas
Positive numbers → Player 1's pieces
Negative numbers → Player 2's pieces

 **How to Run the Project**
**Requirements**
Java JDK 11 or above
Two players (or two terminals for testing)
**Step 1: Compile**

Linux/Mac

chmod +x compile.sh run_server.sh run_client.sh
./compile.sh

Windows

compile.bat

**Step 2: Start Server**

Linux/Mac

./run_server.sh

Windows

run_server.bat

If successful, you will see:

Server listening on port 5555
**Step 3: Start Clients**

Run the client twice:

run_client.sh
**Enter server IP address**
**Click Connect & Play**
Game starts when both players join
**AWS Deployment (EC2)**

To run online gameplay:

Create an EC2 Ubuntu instance
Open port 5555 in Security Group (TCP allowed from anywhere)
Install Java:
sudo apt update && sudo apt install openjdk-17-jdk -y
Upload project:
scp -r out/ ubuntu@<EC2-IP>:~/backgammon/
Run server:
java -cp out server.BackgammonServer

Tip: Use nohup or screen so server keeps running after logout.

Clients connect using EC2 public IP
 **Game Features**
Standard Backgammon rules
Dice rolling system
Turn-based multiplayer gameplay
Bar handling (re-entry required)
Blocking rules (2+ checkers)
Bearing off system
Win detection
Chat system between players
Resign option
Replay option without restarting
 **GitHub Setup**
git init
git remote add origin https://github.com/<yourname>/backgammon-network.git
git add .
git commit -m "Initial project version"
git push -u origin main

Tip: Commit often to show development progress.

 **Submission File Name**
aleena_khan_2521051704_networklab_2026_project.zip


