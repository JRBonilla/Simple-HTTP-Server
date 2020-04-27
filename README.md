# Simple-HTTP-Server

Simple HTTP server that runs on a specified port number, serves HTML pages, and can log all HTTP requests to the console.

The server is capable of handling HEAD, GET, and POST requests and 400, 403, 404, and 501 errors.

## Requirements

Java SE Development Kit.

## How to compile:

Download and install the latest version of the Java SE Development Kit.

Download and extract the files to a folder.

Copy 'html/', 'Main.java', and 'myhttpd.conf' to the same folder.

Open the command line in this folder and compile Main.java using:

> javac Main.java

## How to run

In the command line, run the following command:

> java Main -p port [-d]

```
-p port    Specifies the port number to run the server on
-d         Enable debugging output. Logs all HTTP requests to the console.
 ```
 
 In your web browser, navigate to http://localhost:port/, where port is port number you specified.
