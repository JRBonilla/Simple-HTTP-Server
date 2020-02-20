package com.cps730;

public class Main {

    public static void main(String[] args) {
        boolean debuggingOutput = false;
        int port;

        // Parse command-line arguments
        try {
            // Parse port number
            if (args[0].equals("-p")) {
                port = Integer.parseInt(args[1]);
            } else {
                throw new IllegalArgumentException("Illegal Argument: First argument must be '-p [port]'!");
            }

            // Enable debugging output if specified
            if (args.length == 3) {
                if (args[2].equals("-d")) {
                    debuggingOutput = true;
                } else {
                    throw new IllegalArgumentException("Illegal Argument: Unknown argument " + args[2] + "!");
                }
            }

            Server server = new Server(port, debuggingOutput);
        } catch (IndexOutOfBoundsException e) {
            // No arguments provided, print usage message.
            System.out.println("Usage: SimpleServer -p port [-d]");
        }
    }
}
