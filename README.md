# distributed-ass2-Decentralized-Chat
Distributed system assignment 2

# Model of the decentralized chat app
The connection topology is shown below:
![image](https://user-images.githubusercontent.com/69796042/169293881-a161c972-f839-44c0-a571-f211942d8891.gif)


The connection pattern is decentralized pattern, one peer can only join one room of a peer at the same time, and this peer can have multiple room member peer connections. There is a “hierarchy” in this system, for example, Peer C is a room member of Peer B, and Peer B is also a room member of Peer A, however, such kind of “tiers” are flexible, which means that a peer can disconnect from previous “upper” peer and then connect to whatever peers they want, so that new combinations of “hierarchy” could be established.

Inside a peer, the architecture is shown in the figure below:
![image](https://user-images.githubusercontent.com/69796042/169293993-d59b7f83-1cc7-4d20-9eae-39aea87c1efa.gif)


Based on this architecture, one peer will have two roles: chat room server and chat room member client. For example, other peers can connect to Peer B’s listen port (as is shown in the figure) through NIO multiplexing network model, then other peers can send non-local commands, such as #join, #list to the listen port. The command message goes into the message queue, then server thread will handle these commands in FIFO order with help of some inside modules including Neighbor Manager and Room Manager. Next, the request handler will encode the result into JSON message with Message Service, then the message with be send back to the corresponding client peer.

On the other hand, client thread is a façade. It takes keyboard input, handle local command with managers or encode remote command/chat message into JSON for delivery. Also, client thread will connect to a upper room peer and take incoming message from outgoing port and display with specified format on the screen. 

Client thread also deals with extended features like BFS network crawling, it connects to all neighbors’ listen port, sends list neighbors command and collects response. The response will be stored in a queue for further digging, until all reachable peers are connected and got response from for one time exactly. Based on the result, the client will display all IP + listen port information on the screen.

We use NIO multiplex model to handle incoming client connection without block. As is shown below:
![image](https://user-images.githubusercontent.com/69796042/169294083-797b93fa-e9c8-4351-891e-ec3d20fb5e49.gif)


When a new connection has arrived, server will encapsulate it into Peer model and store it in the Room/ Neighbor Manager. One selector thread will serve for all existing connections, read input messages and put them in the blocking queue to let server handle messages in FIFO order. The response will be written directly through socket channel stored in Room/Neighbor Manager. Server also checks kicked client. If the client’s IP and port is in the blacklist, server will close the channel immediately.

As for upper server communication, the Neighbor will encapsulate Upper server’s address into a Peer model and communicate with it with the corresponding socket channel.
