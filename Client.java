import java.net.Socket;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.BufferedOutputStream;

public class Client implements Runnable {

  private Socket clientSocket;
  private Thread thread;

  public Client(Socket clientSocket) {
    this.clientSocket = clientSocket;
    thread = new Thread(this);
  }

  public void run() {
    System.out.println("Processing client.");
    System.out.println(clientSocket);
    InputStream in = null;
    try {
      in = clientSocket.getInputStream();
    } catch (IOException ioe) {
      System.out.println("Error in InputStream" + ioe);
    }

    while (true) {
      try {
        if (in != null) {
         int unicode = in.read();
         char symbol = (char)unicode;
         System.out.print(symbol);
         if (in.available() == 0) {
           break;
         }
        }
      } catch (IOException ioe) {
        System.out.println("Error " + ioe);
      }
    }

    // System.out.println("All data is read from User Agent.");

    try {
      String message = "<h1> Hello World </h1>";

      message = """
      <!DOCTYPE html>
<html>
<head>
  <title>Login Form</title>
</head>
<body>
  <h2>Login Page</h2>
  <form action="/login_handler" method="POST">
      <div>
          <label for="username">Username:</label>
          <input type="text" id="username" name="username" required>
      </div>
      <br>
      <div>
          <label for="password">Password:</label>
          <input type="password" id="password" name="password" required>
      </div>
      <br>
      <button type="submit">Login</button>
  </form>
</body>
</html>


      """;
      byte[] data = message.getBytes();
      int fileLength = (int) message.length();

      OutputStream out = clientSocket.getOutputStream();
      PrintWriter printWriter = new PrintWriter(out, true);
      printWriter.println("HTTP/1.1 200 OK");
      printWriter.println("Server: Java HTTP Server from Intern Labs 7.0 - Java Backend Developer");
      printWriter.println("Content-type: text/html");
      printWriter.println("Content-length " + fileLength);
      printWriter.println();
      printWriter.flush();

      BufferedOutputStream dataOut = new BufferedOutputStream(out);
      dataOut.write(data, 0, fileLength);
      dataOut.flush();
   } catch(IOException ioe) {
     System.out.println("Error in OutputStream");
   }
  }

  public void go() {
    thread.start();
  }
}
