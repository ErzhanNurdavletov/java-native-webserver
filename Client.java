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
    StringBuilder requestBuilder = new StringBuilder();
    while (true) {
      try {
        if (in != null) {
         int unicode = in.read();
         if (unicode == -1) break;
         char symbol = (char)unicode;
         requestBuilder.append(symbol);
         System.out.print(symbol);
         if (in.available() == 0) {
           break;
         }
        }
      } catch (IOException ioe) {
        System.out.println("Error " + ioe);
      }
    }
    String request = requestBuilder.toString();
    try {
      // Вызываем обработчик из нового класса
      Handler.processRequest(request, clientSocket);
   } catch(IOException ioe) {
     System.out.println("Error in OutputStream");
   }
  }
  public void go() {
    thread.start();
  }
}
