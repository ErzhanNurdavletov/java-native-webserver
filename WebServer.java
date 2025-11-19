import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WebServer {

  private ServerSocket serverSocket;
  private final Object lock;
  private final RequestHandler requestHandler;

  WebServer(int port, RequestHandler requestHandler) {
    this.requestHandler = requestHandler;
    lock = new Object();
    try {
      serverSocket = new ServerSocket(port);
    } catch (IOException ioe) {
      System.out.println("Server socket error " + ioe);
    }
  }

  public void startServer() {
    while (true) {
      try {
        Socket socket = serverSocket.accept();
        synchronized(lock) {
          Client client = new Client(socket, requestHandler);
          client.go();
        }
      } catch (IOException ioe) {
        System.out.println("Socket error " + ioe);
      }
    }
  }
}
