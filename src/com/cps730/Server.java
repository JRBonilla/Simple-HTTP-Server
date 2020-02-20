package com.cps730;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.AccessDeniedException;
import java.util.Date;
import java.util.Objects;
import java.util.StringTokenizer;

public class Server {

    // Server specific variables
    private static final String BAD_REQUEST     = "400.html";
    private static final String FORBIDDEN       = "403.html";
    private static final String NOT_FOUND       = "404.html";
    private static final String NOT_IMPLEMENTED = "501.html";
    private static final String HOME_PAGE       = "index.html";
    private static final Date   DATE            = new Date();
    private static final File   WEB_ROOT        = new File(Objects.requireNonNull(ParseWebRoot()));

    // Socket specific variables
    private ServerSocket         server     = null;
    private Socket               socket     = null;
    private PrintWriter          writer     = null;
    private InputStreamReader    isr        = null;
    private BufferedReader       reader     = null;
    private BufferedOutputStream dataOutput = null;

    // Constructor takes in port # and whether debugging output is enabled or not
    public Server(int port, boolean debugging) {
        try {
            // Initialize server
            server = new ServerSocket(port);
            System.out.println("Server started");

            System.out.println("Listening for connection on port " + port + "...");
            while (true) {
                // Accept client connection
                socket = server.accept();
                System.out.println("Client accepted");

                // Initialize socket variables
                writer = new PrintWriter(socket.getOutputStream());
                isr = new InputStreamReader(socket.getInputStream());
                reader = new BufferedReader(isr);
                dataOutput = new BufferedOutputStream(socket.getOutputStream());

                // Parse the HTTP request
                String line = reader.readLine();
                StringTokenizer requestParser = new StringTokenizer(line);
                String method = requestParser.nextToken().toUpperCase();
                String requestURI = requestParser.nextToken().toLowerCase();
                String version = requestParser.nextToken().toUpperCase();

                // If debugging is enabled, print the request
                if (debugging) {
                    System.out.println("-----------------------------------------------------------------------------");
                    while (!line.isEmpty()) {
                        System.out.println(line);
                        line = reader.readLine();
                    }
                    System.out.println("-----------------------------------------------------------------------------");
                }

                // Only GET, HEAD, and POST methods are supported
                if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
                    // Return the 501 Error page to the client
                    HTTPResponse501();
                }
                // Bad request submitted
                else if (!requestURI.startsWith("/") && version.equals("HTTP/2.0")) {
                    HTTPResponse400();
                }
                else {
                    // If only "/" is requested, redirect the user to index.html
                    if (requestURI.endsWith("/")) {
                        requestURI += HOME_PAGE;
                    }

                    // Retrieve the requested file
                    File file = new File(WEB_ROOT, requestURI);
                    int length = (int) file.length();
                    String contentType = GetContentType(requestURI);

                    // If the GET method is used, return the file to the user
                    if (method.equals("GET")) {
                        HTTPResponse200(file);
                    }
                    else if (method.equals("POST")) {

                    }
                }
            }
        } catch (AccessDeniedException e) {
            try {
                HTTPResponse403();
            } catch (IOException i) {
                System.out.println("Error: Could not send 403 message!");
                System.out.println(e.getMessage());
            }
        } catch (FileNotFoundException e) {
            try {
                HTTPResponse404();
            } catch (IOException i) {
                System.out.println("Error: Could not send 404 message!");
                System.out.println(e.getMessage());
            }
        } catch (IOException e) {
            System.out.println("IO Error: " + e.getMessage());
        } finally {
            try {
                reader.close();
                writer.close();
                dataOutput.close();
                socket.close();
            } catch (Exception e) {
                System.out.println("Error: Could not close stream!");
                System.out.println(e.getMessage());
            }
        }
    }

    // Returns the content type of the file
    private String GetContentType(String file) {
        if (file.endsWith(".htm") || file.endsWith(".html")) {
            return "text/html";
        }
        else {
            return "text/plain";
        }
    }

    // Generates an HTTP response depending on the code
    private void GenerateHTTPResponse(int code, String contentType, int length) {
        // Throw an exception if the code is not supported
        if (code != 200 && code != 201 && code != 400 && code != 403 && code != 404 && code != 501) {
            throw new IllegalArgumentException("Illegal Argument: Unknown code " + code + "!");
        }

        // Set message depending on the code
        String message = "";
        switch (code) {
            case 200:
                message = "OK";
                break;
            case 201:
                message = "Created";
                break;
            case 400:
                message = "Bad Request";
                break;
            case 403:
                message = "Forbidden";
                break;
            case 404:
                message = "Not Found";
                break;
            case 501:
                message = "Not Implemented";
                break;
        }

        // Generate the HTTP response
        writer.println("HTTP/1.1 " + code + " " + message);
        writer.println("Server: CPS730 Java HTTP Server");
        writer.println("Date" + DATE);
        writer.println("Content-type: " + contentType);
        writer.println("Content-length: " + length);
        writer.println();
        writer.flush();
    }

    // OK response
    private void HTTPResponse200(File file) throws IOException {
        byte[] fileBytes = ReadFile(file, (int)file.length());
        String contentType = GetContentType(file.getName());

        GenerateHTTPResponse(200, contentType, (int)file.length());
        dataOutput.write(fileBytes, 0, (int)file.length());
        dataOutput.flush();
    }

    // Created response
    private void HTTPResponse201(File file) throws IOException {
        GenerateHTTPResponse(201, GetContentType(file.getName()), (int)file.length());
    }

    // Bad request error
    private void HTTPResponse400() throws IOException {
        File file = new File(WEB_ROOT, BAD_REQUEST);
        int length = (int) file.length();
        String contentType = "text/html";
        byte[] fileBytes = ReadFile(file, length);

        GenerateHTTPResponse(400, contentType, length);
        dataOutput.write(fileBytes, 0, length);
        dataOutput.flush();
    }

    // Forbidden error
    private void HTTPResponse403() throws IOException {
        File file = new File(WEB_ROOT, FORBIDDEN);
        int length = (int) file.length();
        String contentType = "text/html";
        byte[] fileBytes = ReadFile(file, length);

        GenerateHTTPResponse(403, contentType, length);
        dataOutput.write(fileBytes, 0, length);
        dataOutput.flush();
    }

    // File not found error
    private void HTTPResponse404() throws IOException {
        File file = new File(WEB_ROOT, NOT_FOUND);
        int length = (int) file.length();
        String contentType = "text/html";
        byte[] fileBytes = ReadFile(file, length);

        GenerateHTTPResponse(404, contentType, length);
        dataOutput.write(fileBytes, 0, length);
        dataOutput.flush();
    }

    // Method not implemented error
    private void HTTPResponse501() throws IOException {
        File file = new File(WEB_ROOT, NOT_IMPLEMENTED);
        int length = (int) file.length();
        String contentType = "text/html";
        byte[] fileBytes = ReadFile(file, length);

        GenerateHTTPResponse(501, contentType, length);
        dataOutput.write(fileBytes, 0, length);
        dataOutput.flush();
    }

    // Reads the specified file and returns the data in a byte array
    private byte[] ReadFile(File file, int length) throws IOException {
        FileInputStream fileInputStream = null;
        byte[] data = new byte[length];
        try {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(data);
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
        return data;
    }

    // Parses the config file for the web root
    private static String ParseWebRoot() {
        try {
            File file = new File("myhttpd.conf");
            BufferedReader reader = new BufferedReader(new FileReader(file));

            String line = reader.readLine();
            StringTokenizer tokenizer = new StringTokenizer(line);
            String version = tokenizer.nextToken().toUpperCase();

            System.out.println("Successfully loaded config file myhttpd.conf");
            return tokenizer.nextToken().toLowerCase();
        } catch (IOException e) {
            System.out.println("Error: Could not load config file myhttpd.conf!");
            System.out.println(e.getMessage());
        }
        return null;
    }

}
