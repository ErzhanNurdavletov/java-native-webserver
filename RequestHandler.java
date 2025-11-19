import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class RequestHandler {

  private DataStore dataStore;
  private UserService userService;
  private SessionService sessionService;
  private GuestbookService guestbookService;
  private ResponseUtil responseUtil;

  public RequestHandler(DataStore dataStore, UserService userService, SessionService sessionService, GuestbookService guestbookService, ResponseUtil responseUtil) {
    this.dataStore = dataStore;
    this.userService = userService;
    this.sessionService = sessionService;
    this.guestbookService = guestbookService;
    this.responseUtil = responseUtil;
  }

  public void processRequest(String request, Socket clientSocket) throws IOException {
    OutputStream outputStream = clientSocket.getOutputStream();
    PrintWriter printWriter = new PrintWriter(outputStream, true);

    RequestParser requestParser = new RequestParser();

    try {
      requestParser.parse(request);

      String method = requestParser.getMethod();
      String path = requestParser.getPath();
      String sessionToken = requestParser.getCookie("session");

      if (method.isEmpty()) {
        responseUtil.sendResponse(printWriter, outputStream, 400, "Bad Request", "text/plain", "Empty request".getBytes());
        return;
      }
      if ("/styles.css".equals(path)) {
        byte[] cssData = dataStore.readFileBytes("styles.css");
        if (cssData != null) {
          responseUtil.sendResponse(printWriter, outputStream, 200, "OK", "text/css", cssData);
        } else {
          responseUtil.sendResponse(printWriter, outputStream, 404, "Not Found", "text/plain", "CSS not found".getBytes());
        }
        return;
      }
      if ("GET".equals(method)) {
        if ("/".equals(path) || path.isEmpty()) {
          String loginPage = dataStore.readFileContent("login.html");
          responseUtil.sendResponse(printWriter, outputStream, 200, "OK", "text/html", loginPage.getBytes());
        } else if ("/register".equals(path)) {
          String registerPage = dataStore.readFileContent("register.html");
          responseUtil.sendResponse(printWriter, outputStream, 200, "OK", "text/html", registerPage.getBytes());
        } else if ("/guestbook".equals(path)) {
          if (sessionService.isSessionValid(sessionToken)) {
            String template = dataStore.readFileContent("guestbook.html");
            String messagesList = guestbookService.generateMessagesList();
            String page = template.replace("<!-- MESSAGES_LIST -->", messagesList);
            responseUtil.sendResponse(printWriter, outputStream, 200, "OK", "text/html", page.getBytes());
          } else {
            responseUtil.sendRedirect(printWriter, "/", null);
          }
        } else if ("/logout".equals(path)) {
          sessionService.deleteSession(sessionToken);
          responseUtil.sendRedirect(printWriter, "/", "Set-Cookie: session=; Path=/; Max-Age=0");
        } else {
          responseUtil.sendResponse(printWriter, outputStream, 404, "Not Found", "text/plain", ("Not found: " + path).getBytes());
        }
        return;
      }
      if ("POST".equals(method)) {
        String body = requestParser.getBody();
        Map<String, String> formData = requestParser.parseFormData();
        String username = formData.get("username");
        String password = formData.get("password");

        if ("/login".equals(path)) {
          if (userService.validateUser(username, password)) {
            String token = sessionService.createSession(username);
            responseUtil.sendRedirect(printWriter, "/guestbook", "Set-Cookie: session=" + token + "; Path=/; Max-Age=3600");
          } else {
            responseUtil.sendResponse(printWriter, outputStream, 401, "Unauthorized", "text/html", "<h1>Invalid credentials</h1><a href='/'>Try again</a>".getBytes());
          }
        } else if ("/register".equals(path)) {
          if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            if (userService.userExists(username)) {
              responseUtil.sendResponse(printWriter, outputStream, 409, "Conflict", "text/html", "<h1></h1>Login is already exist <a href='/register'>try another</a>".getBytes());
            } else {
              userService.saveUser(username, password);
              responseUtil.sendRedirect(printWriter, "/", null);
            }
          } else {
            responseUtil.sendResponse(printWriter, outputStream, 400, "Bad Request", "text/html", "<h1>Incorect data</h1><a href='/register'>return</a>".getBytes());
          }
        } else if ("/submit".equals(path)) {
          if (sessionService.isSessionValid(sessionToken)) {
            String name = formData.get("name");
            String messageText = formData.get("message");
            if (name != null && !name.isEmpty() && messageText != null && !messageText.isEmpty()) {
              guestbookService.saveMessage(name, messageText);
            }
            responseUtil.sendRedirect(printWriter, "/guestbook", null);
          } else {
            responseUtil.sendResponse(printWriter, outputStream, 403, "Forbidden", "text/plain", "Unauthorized".getBytes());
          }
        } else {
          responseUtil.sendResponse(printWriter, outputStream, 404, "Not Found", "text/plain", ("Not found: " + path).getBytes());
        }
        return;
      }
      responseUtil.sendResponse(printWriter, outputStream, 405, "Method Not Allowed", "text/plain", ("Method not supported: " + method).getBytes());

    } catch (Exception e) {
      System.out.println("Error processing request: " + e);
      responseUtil.sendResponse(printWriter, outputStream, 500, "Internal Server Error", "text/plain", "Server error".getBytes());
    } finally {
      outputStream.close();
    }
  }
}
