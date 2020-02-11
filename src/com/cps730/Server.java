package com.cps730;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket      server = null;
    private Socket            socket = null;
    private InputStreamReader isr    = null;
    private BufferedReader    reader = null;

    public Server(int port) {
        try {
            server = new ServerSocket(port);
            System.out.println("Server started");

            System.out.println("Listening for connection on port " + port + "...");
            while (true) {
                socket = server.accept();
                System.out.println("Client accepted");

                isr = new InputStreamReader(socket.getInputStream());
                reader = new BufferedReader(isr);

                String line = reader.readLine();
                while (!line.isEmpty()) {

                }

                socket.close();
                reader.close();
                isr.close();
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

}
