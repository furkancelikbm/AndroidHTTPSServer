# AndroidHTTPSServer

 This project implements a secure Android-based server that mimics the behavior of a Point-of-Sale (POS) terminal, designed to run within a local area network (LAN) environment. The server handles client communication using mutual TLS (mTLS), which ensures end-to-end security by requiring both server and client certificate authentication. Only clients with valid, CA-signed certificates can connect to the server.

Upon receiving an HTTPS POST request from a trusted client, the server parses the incoming JSON-formatted receipt data through a dedicated user interface (receipt parser UI). This UI allows the operator to visually inspect and verify the received transaction.

Once validated, the parsed data is saved to a local SQLite database on the Android device for secure storage, logging, and further processing. Each receipt entry includes structured transaction details such as product name, quantity, price, total, timestamp, and any associated metadata.

![WhatsApp Görsel 2025-04-16 saat 19 38 38_e8d8e06d](https://github.com/user-attachments/assets/d26fb56b-9ba4-4844-8e85-4386206d2c61)



https://github.com/user-attachments/assets/e750647b-d5d7-47f7-a8c5-6101a7002d25

