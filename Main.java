public class Main {
  public static void main(String[] args) {
    ServiceContainer container = new ServiceContainer();
    RequestHandler requestHandler = container.getRequestHandler();
    int port = 8080;
    WebServer webServer = new WebServer(port, requestHandler);
    System.out.println("Server started on port " + port);
    webServer.startServer();
  }
}
