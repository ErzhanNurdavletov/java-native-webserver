import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class WebServer {

  private ServerSocket serverSocket;
  private Object lock;
  private boolean flag;

  WebServer(int port) {
    try {
      serverSocket = new ServerSocket(port);
      lock = new Object();
    } catch (IOException ioe) {
      System.out.println("Server socket error " + ioe);
    }
  }

  public void startServer() {
    while (true) {
      try {
        Socket socket = serverSocket.accept();
        synchronized(lock) {
          Client client = new Client(socket);
          client.go();
        }
      } catch (IOException ioe) {
        System.out.println("Socket error " + ioe);
      }
    }
  }
}
