public class Main {
  public static void main(String[] args) {
    int port = 8080;
    WebServer webServer = new WebServer(port);
    System.out.println("Server started on port " + port);
    webServer.startServer();
  }
}
