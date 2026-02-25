# Group Chat Application

A real-time group chat application built with **Java Sockets (TCP)** and **JavaFX**, following a Server-Client architecture. Multiple clients can connect to a central server and exchange messages in a shared chat environment.

---

## Architecture

The application strictly follows **Model-View separation** — socket/network logic is fully independent of the JavaFX UI layer.

---

## App Features

**Client**
- Username-based authentication before entering the chat
- Read-only mode for anonymous (no username) connections
- Send messages via button or Enter key
- `allUsers` command to list all active users
- Disconnect with `end` or `bye`
- Online status indicator in the UI

**Server**
- Handles multiple clients concurrently (thread-per-connection)
- Broadcasts messages formatted as `[username @ HH:mm]: message`
- Live ListView of connected users with random color coding
- Activity log (server start, client connections, disconnections)


---
## Running the Application
**Step 1: Start the Server:**
```bash
cd TCPServer
mvn javafx:run
````
When the server UI opens, click "Start Server". 
You should see:

Server Started on port 3000

Waiting for clients...

**Step 2: Start a Client:**

Open a new terminal tab/window:
```bash
bashcd TCPClient
mvn javafx:run
```

**Step 3 : Connect**

In the client login screen:

Enter a username (or leave empty for read-only mode)

Server IP and Port are pre-filled (localhost and 3000)

Click Connect

**Step 4 : Test multiple clients**

Open another terminal and run the client again with a different username. Messages sent from one client will appear in all connected clients.

---

## 👥 Team

This project was developed as part of a university assignment under the supervision of **El Habib Nfaoui**.

<!-- Add team members below -->
- Aya Dafir
- Douaa Britel
- Maria Chmite
- Noha Lakhmarti
- Kenza Qribis

