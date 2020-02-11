package com.cps730;

public class Main {

    public static void main(String[] args) {
        int port = 8080;
        boolean debugging = false;

        // Parse command-line arguments
        try {
            // Parse port number
            if (args[0].equals("-p")) {
                port = Integer.parseInt(args[1]);
            } else {
                throw new IllegalArgumentException("Illegal Argument: First argument must be '-p [port]'!");
            }

            // Enable debugging output if specified
            if (args.length > 3) {
                if (args[2].equals("-d")) {
                    debugging = true;
                } else {
                    throw new IllegalArgumentException("Illegal Argument: Unknown argument!");
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // No arguments provided, print usage message.
            System.out.println("Usage: SimpleServer -p port [-d]");
        }

        Server server = new Server(port);
    }
}
