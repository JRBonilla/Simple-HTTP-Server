import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class Main {

    private static boolean debuggingOutput = false;
    private static ArrayList<String> FileTypes = new ArrayList<String>();

    private static final String BAD_REQUEST     = "html/400.html";
    private static final String FORBIDDEN       = "html/403.html";
    private static final String NOT_FOUND       = "html/404.html";
    private static final String NOT_IMPLEMENTED = "html/501.html";
    private static final String HOME_PAGE       = "html/index.html";
    private static final String WEB_ROOT        = ParseWebRoot();

    public static void main(String[] args) throws IOException {
        int port = 0;

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
            // Initialize the server
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext(WEB_ROOT, Main::HandleRequest);
            server.setExecutor(null);
            server.start();

            if (debuggingOutput) {
                System.out.println("Server started");
                System.out.println("Listening for connection on port " + port + "...");
            }
        } catch (IndexOutOfBoundsException e) {
            // No arguments provided, print usage message.
            System.out.println("Usage: SimpleServer -p port [-d]");
        }
    }

    // Parses the config file for the web root
    private static String ParseWebRoot() {
        try {
            File file = new File("myhttpd.conf");
            BufferedReader reader = new BufferedReader(new FileReader(file));

            String line = reader.readLine();
            StringTokenizer tokenizer = new StringTokenizer(line);
            String version = tokenizer.nextToken().toUpperCase();
            String webRoot = tokenizer.nextToken().toLowerCase();

            line = reader.readLine();
            tokenizer = new StringTokenizer(line);
            while (tokenizer.hasMoreTokens()) {
                String fileType = tokenizer.nextToken();
                FileTypes.add(fileType);
            }

            System.out.println("Successfully loaded config file myhttpd.conf");
            return webRoot;
        } catch (IOException e) {
            System.out.println("Error: Could not load config file myhttpd.conf!");
            System.out.println(e.getMessage());
        }
        return null;
    }

    // Reads the specified file and returns the data in a byte array
    private static byte[] ReadFile(File file, int length) throws IOException {
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

    // Method that decides how to handle HTTP requests
    private static void HandleRequest(HttpExchange exchange) throws IOException {
        switch (exchange.getRequestMethod().toUpperCase()) {
            case "HEAD":
                HandleHEADRequest(exchange);
                break;
            case "GET":
                HandleGETRequest(exchange);
                break;
            case "POST":
                HandlePOSTRequest(exchange);
                break;
            default:
                // Error 501: Only HEAD, POST, and GET methods are implemented
                SendHTTPError(exchange, HttpURLConnection.HTTP_NOT_IMPLEMENTED, NOT_IMPLEMENTED);
                break;
        }
    }

    // Handles HTTP GET requests
    private static void HandleGETRequest(HttpExchange exchange) throws IOException {
        if (debuggingOutput) {
            System.out.println("Client accepted");
            PrintHTTPRequest(exchange);
        }

        URI requestURI = exchange.getRequestURI();
        String requestPath = requestURI.getPath();

        String version = exchange.getProtocol().toUpperCase();

        // Bad request was made, send a 400 error
        if (!requestPath.startsWith("/") || version.equals("HTTP/2.0")) {
            SendHTTPError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, BAD_REQUEST);
        }
        else {
            if (requestPath.endsWith("/")) {
                requestPath += HOME_PAGE;
            }

            if (!SupportedFileType(GetFileExtension(requestPath))) {
                SendHTTPError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, BAD_REQUEST);
            }
            else {
                // The file was not found, so return a 404 error page
                if (!Files.exists(Paths.get("." + requestPath))) {
                    SendHTTPError(exchange, HttpURLConnection.HTTP_NOT_FOUND, NOT_FOUND);
                }
                // The file cannot be written to, so return a 403 error page
                if (!Files.isWritable(Paths.get("." + requestPath))) {
                    SendHTTPError(exchange, HttpURLConnection.HTTP_FORBIDDEN, FORBIDDEN);
                }
                // Otherwise, send the file and OK
                else {
                    File requestedFile = new File("." + WEB_ROOT, requestPath);
                    byte[] fileBytes = ReadFile(requestedFile, (int) requestedFile.length());
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, fileBytes.length);
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(fileBytes);
                    outputStream.close();
                }
            }
        }
    }

    // Handles HTTP head requests
    private static void HandleHEADRequest(HttpExchange exchange) throws IOException {
        if (debuggingOutput) {
            System.out.println("Client accepted");
            PrintHTTPRequest(exchange);
        }
        URI requestURI = exchange.getRequestURI();
        String requestPath = requestURI.getPath();

        String version = exchange.getProtocol().toUpperCase();

        // Bad request was made, send a 400 error
        if (!requestPath.startsWith("/") || version.equals("HTTP/2.0")) {
            SendHTTPError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, BAD_REQUEST);
        }
        else {
            if (requestPath.endsWith("/")) {
                requestPath += HOME_PAGE;
            }

            if (!SupportedFileType(GetFileExtension(requestPath))) {
                SendHTTPError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, BAD_REQUEST);
            }
            else {
                // The file was not found, so return a 404 error page
                if (!Files.exists(Paths.get("." + requestPath))) {
                    SendHTTPError(exchange, HttpURLConnection.HTTP_NOT_FOUND, NOT_FOUND);
                }
                // The file cannot be written to, so return a 403 error page
                else if (!Files.isWritable(Paths.get("." + requestPath))) {
                    SendHTTPError(exchange, HttpURLConnection.HTTP_FORBIDDEN, FORBIDDEN);
                }
                // Otherwise, send OK and no file
                else {
                    File requestedFile = new File("." + WEB_ROOT, requestPath);
                    byte[] fileBytes = ReadFile(requestedFile, (int) requestedFile.length());
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, fileBytes.length);
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(0);
                    outputStream.close();
                }
            }
        }
    }

    // Handles HTTP POST requests
    private static void HandlePOSTRequest(HttpExchange exchange) throws IOException {
        if (debuggingOutput) {
            System.out.println("Client accepted");
            PrintHTTPRequest(exchange);
        }

        URI requestURI = exchange.getRequestURI();
        String requestPath = requestURI.getPath();

        String version = exchange.getProtocol().toUpperCase();

        // Bad request was made, send a 400 error
        if (!requestPath.startsWith("/") || version.equals("HTTP/2.0")) {
            SendHTTPError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, BAD_REQUEST);
        }
        else {
            Headers requestHeaders = exchange.getRequestHeaders();
            int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));

            InputStream inputStream = exchange.getRequestBody();
            byte[] data = new byte[contentLength];
            int length = inputStream.read(data);

            String[] dataString = new String(data).split("=");
            String fileName = dataString[1];
            File file = new File(fileName);
            if (file.createNewFile()) {
                if (debuggingOutput) {
                    System.out.println("Successfully created " + fileName);
                }

                exchange.sendResponseHeaders(HttpURLConnection.HTTP_CREATED, contentLength);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(data);
                outputStream.close();
            }
        }
    }

    // Sends an HTML error page to the user given a response code and a error file.
    private static void SendHTTPError(HttpExchange exchange, int responseCode, String errorFile) throws IOException {
        File requestedFile = new File("." + WEB_ROOT, errorFile);
        byte[] fileBytes = ReadFile(requestedFile, (int) requestedFile.length());

        exchange.sendResponseHeaders(responseCode, fileBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(fileBytes);
        outputStream.close();
    }

    // Prints the HTTP request to the console
    private static void PrintHTTPRequest(HttpExchange exchange) {
        System.out.println("-----------------------------------------------------------------------------------------");

        String requestMethod = exchange.getRequestMethod();
        String requestURI = exchange.getRequestURI().getPath();
        String version = exchange.getProtocol();
        System.out.println(requestMethod + " " + requestURI + " " + version);

        Headers requestHeaders = exchange.getRequestHeaders();
        requestHeaders.entrySet().forEach(System.out::println);

        System.out.println("-----------------------------------------------------------------------------------------");
    }

    // Checks if the file type is supported
    private static boolean SupportedFileType(String fileType) {
        for (String supported : FileTypes) {
            if (fileType.equals(supported)) {
                return true;
            }
        }
        return false;
    }

    // Returns the file extension
    private static String GetFileExtension(String path) {
        String file = path.substring(path.lastIndexOf("/"));
        return file.substring(file.indexOf(".")).substring(1);
    }

}
