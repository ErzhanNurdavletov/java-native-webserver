import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class Client implements Runnable {
  private Socket clientSocket;
  private Thread thread;
  private RequestHandler requestHandler;

  public Client(Socket clientSocket, RequestHandler requestHandler) {
    this.clientSocket = clientSocket;
    this.requestHandler = requestHandler;
    thread = new Thread(this);
  }

  public void run() {
    System.out.println("Processing client.");
    System.out.println(clientSocket);

    InputStream inputStream = null;
    try {
      inputStream = clientSocket.getInputStream();
    } catch (IOException ioe) {
      System.out.println("Error in InputStream" + ioe);
      return;
    }
    StringBuilder requestBuilder = new StringBuilder();
    try {
      while (true) {
        int unicode = inputStream.read();
        if (unicode == -1) {
          break;
        }
        char symbol = (char) unicode;
        requestBuilder.append(symbol);
        System.out.print(symbol);
        if (inputStream.available() == 0) {
          break;
        }
      }
    } catch (IOException ioe) {
      System.out.println("Error reading request: " + ioe);
    }

    String request = requestBuilder.toString();
    try {
      requestHandler.processRequest(request, clientSocket);
    } catch(IOException ioe) {
      System.out.println("Error processing request in Handler: " + ioe);
    } finally {
      try {
        clientSocket.close();
      } catch (IOException e) {
        System.out.println("Error closing socket: " + e);
      }
    }
  }

  public void go() {
    thread.start();
  }
}
