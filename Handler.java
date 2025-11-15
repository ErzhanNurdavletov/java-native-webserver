import java.io.*;
import java.util.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Handler {

  public static void processRequest(String request, Socket clientSocket) throws IOException {

    OutputStream out = clientSocket.getOutputStream();
    PrintWriter printWriter = new PrintWriter(out, true);

    try {
      // Парсим запрос из строки
      String[] lines = request.split("\r\n");
      if (lines.length == 0) {
        sendResponse(printWriter, out, 400, "Bad Request", "text/plain", "Empty request".getBytes());
        return;
      }
      String[] requestLine = lines[0].split(" ");
      String method = requestLine[0];
      String path = requestLine[1];
      if (path.contains("?")) {
        path = path.substring(0, path.indexOf("?"));
      }
      // Парсим заголовки
      Map<String, String> headers = new HashMap<>();
      int bodyStart = -1;
      for (int i = 1; i < lines.length; i++) {
        if (lines[i].isEmpty()) {
          bodyStart = i + 1;
          break;
        }
        String[] header = lines[i].split(": ", 2);
        if (header.length == 2) {
          headers.put(header[0].toLowerCase(), header[1]);
        }
      }

      // Тело для POST
      String body = "";
      if ("POST".equals(method) && bodyStart > 0 && bodyStart < lines.length) {
        StringBuffer bodyStringBuffer = new StringBuffer();
        for (int i = bodyStart; i < lines.length; i++) {
          bodyStringBuffer.append(lines[i]);
        }
        body = bodyStringBuffer.toString();
      }

      // Парсим куки
      String sessionToken = getCookie(headers.get("cookie"), "session");

      // Обработка маршрутов
      if ("/styles.css".equals(path)) {
        byte[] cssData = readFileBytes("resources/styles.css");
        if (cssData != null) {
          sendResponse(printWriter, out, 200, "OK", "text/css", cssData);
        } else {
          sendResponse(printWriter, out, 404, "Not Found", "text/plain", "CSS not found".getBytes());
        }
      } else if ("GET".equals(method) && ("/".equals(path) || path.isEmpty())) {
        String loginPage = readFileContent("resources/login.html");
        sendResponse(printWriter, out, 200, "OK", "text/html", loginPage.getBytes());
      } else if ("POST".equals(method) && "/login".equals(path)) {
        Map<String, String> formData = parseFormData(body);
        String username = formData.get("username");
        String password = formData.get("password");
        if (validateUser(username, password)) {
          String token = UUID.randomUUID().toString();
          long expiration = System.currentTimeMillis() + 3600000;
          saveSession(token, username, expiration);
          printWriter.println("HTTP/1.1 302 Found");
          printWriter.println("Location: /guestbook");
          printWriter.println("Set-Cookie: session=" + token + "; Path=/; Max-Age=3600");
          printWriter.println();
          printWriter.flush();
        } else {
          sendResponse(printWriter, out, 401, "Unauthorized", "text/html", "<h1>Invalid credentials</h1><a href='/'>Try again</a>".getBytes());
        }
      } else if ("GET".equals(method) && "/register".equals(path)) {
        String registerPage = readFileContent("resources/register.html");
        sendResponse(printWriter, out, 200, "OK", "text/html", registerPage.getBytes());
      } else if ("POST".equals(method) && "/register".equals(path)) {
        Map<String, String> formData = parseFormData(body);
        String username = formData.get("username");
        String password = formData.get("password");
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
          if (userExists(username)) {
            sendResponse(printWriter, out, 409, "Conflict", "text/html", "<h1>Логин уже занят</h1><a href='/register'>Попробовать другой</a>".getBytes());
          } else {
            saveUser(username, password);
            printWriter.println("HTTP/1.1 302 Found");
            printWriter.println("Location: /");
            printWriter.println();
            printWriter.flush();
          }
        } else {
          sendResponse(printWriter, out, 400, "Bad Request", "text/html", "<h1>Некорректные данные</h1><a href='/register'>Вернуться</a>".getBytes());
        }
      } else if ("GET".equals(method) && "/guestbook".equals(path)) {
        if (isSessionValid(sessionToken)) {
          String template = readFileContent("resources/guestbook.html");
          String messagesList = generateMessagesList();
          String page = template.replace("<!-- MESSAGES_LIST -->", messagesList);
          sendResponse(printWriter, out, 200, "OK", "text/html", page.getBytes());
        } else {
          printWriter.println("HTTP/1.1 302 Found");
          printWriter.println("Location: /");
          printWriter.println();
          printWriter.flush();
        }
      } else if ("POST".equals(method) && "/submit".equals(path)) {
        if (isSessionValid(sessionToken)) {
          Map<String, String> formData = parseFormData(body);
          String name = formData.get("name");
          String messageText = formData.get("message");
          if (name != null && !name.isEmpty() && messageText != null && !messageText.isEmpty()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            saveMessage(name, messageText, timestamp);
          }
          printWriter.println("HTTP/1.1 302 Found");
          printWriter.println("Location: /guestbook");
          printWriter.println();
          printWriter.flush();
        } else {
          sendResponse(printWriter, out, 403, "Forbidden", "text/plain", "Unauthorized".getBytes());
        }
      } else if ("GET".equals(method) && "/logout".equals(path)) {
        deleteSession(sessionToken);
        printWriter.println("HTTP/1.1 302 Found");
        printWriter.println("Location: /");
        printWriter.println("Set-Cookie: session=; Path=/; Max-Age=0");
        printWriter.println();
        printWriter.flush();
      } else {
        sendResponse(printWriter, out, 404, "Not Found", "text/plain", ("Not found: " + path).getBytes());
      }
    } catch (Exception e) {
      System.out.println("Error processing request: " + e);
      sendResponse(printWriter, out, 500, "Internal Server Error", "text/plain", "Server error".getBytes());
    } finally {
      out.close();
    }
  }

  // Остальные методы такие же, как в предыдущем варианте (копируй из моего прошлого ответа)
  private static void sendResponse(PrintWriter printWriter, OutputStream out, int code, String text, String type, byte[] data) throws IOException {
    printWriter.println("HTTP/1.1 " + code + " " + text);
    printWriter.println("Server: Java HTTP Server from Intern Labs 7.0 - Java Backend Developer");
    printWriter.println("Content-Type: " + type);
    printWriter.println("Content-Length: " + data.length);
    printWriter.println();
    printWriter.flush();
    BufferedOutputStream dataOut = new BufferedOutputStream(out);
    dataOut.write(data, 0, data.length);
    dataOut.flush();
  }

  private static String readFileContent(String path) {
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path))) {
      StringBuffer stringBuffer = new StringBuffer();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        stringBuffer.append(line).append("\n");
      }
      return stringBuffer.toString();
    } catch (IOException ioe) {
      System.out.println("Error reading file " + path + ": " + ioe);
      return "";
    }
  }

  private static byte[] readFileBytes(String path) {
    try {
      File file = new File(path);
      if (file.exists()) {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
          fis.read(bytes);
        }
        return bytes;
      }
    } catch (IOException ioe) {
      System.out.println("Error reading bytes " + path + ": " + ioe);
    }
    return null;
  }

  private static Map<String, String> parseFormData(String body) {
    Map<String, String> data = new HashMap<>();
    if (body != null) {
      String[] pairs = body.split("&");
      for (String pair : pairs) {
        String[] kv = pair.split("=");
        if (kv.length == 2) {
          data.put(kv[0], kv[1].replace("+", " "));
        }
      }
    }
    return data;
  }

  private static String getCookie(String cookieHeader, String name) {
    if (cookieHeader == null) return null;
    String[] cookies = cookieHeader.split("; ");
    for (String cookie : cookies) {
      if (cookie.startsWith(name + "=")) {
        return cookie.substring(name.length() + 1);
      }
    }
    return null;
  }

  private static boolean validateUser(String username, String password) {
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader("resources/users.txt"))) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        String[] parts = line.split(":");
        if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(password)) {
          return true;
        }
      }
    } catch (IOException ioe) {
      System.out.println("Error validating user: " + ioe);
    }
    return false;
  }

  private static void saveSession(String token, String username, long expiration) {
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("resources/sessions.txt", true))) {
      bufferedWriter.write(token + ":" + username + ":" + expiration);
      bufferedWriter.newLine();
    } catch (IOException ioe) {
      System.out.println("Error saving session: " + ioe);
    }
  }

  private static boolean isSessionValid(String token) {
    if (token == null) return false;
    try (BufferedReader bufferedWriter = new BufferedReader(new FileReader("resources/sessions.txt"))) {
      String line;
      long now = System.currentTimeMillis();
      while ((line = bufferedWriter.readLine()) != null) {
        String[] parts = line.split(":");
        if (parts.length == 3 && parts[0].equals(token) && Long.parseLong(parts[2]) > now) {
          return true;
        }
      }
    } catch (IOException ioe) {
      System.out.println("Error checking session: " + ioe);
    }
    return false;
  }

  private static void deleteSession(String token) {
    if (token == null) {
      return;
    }
    List<String> sessions = new ArrayList<>();
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader("resources/sessions.txt"))) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        if (!line.startsWith(token + ":")) {
          sessions.add(line);
        }
      }
    } catch (IOException ioe) {
      System.out.println("Error reading sessions: " + ioe);
      return;
    }
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("resources/sessions.txt"))) {
      for (String session : sessions) {
        bufferedWriter.write(session);
        bufferedWriter.newLine();
      }
    } catch (IOException ioe) {
      System.out.println("Error writing sessions: " + ioe);
    }
  }

  private static void saveMessage(String username, String message, String time) {
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("resources/messages.txt", true))) {
      bufferedWriter.write(username + ":" + message + ":" + time);
      bufferedWriter.newLine();
    } catch (IOException ioe) {
      System.out.println("Error saving message: " + ioe);
    }
  }

  private static String generateMessagesList() {
    StringBuilder list = new StringBuilder("<ul>");
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader("resources/messages.txt"))) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        String[] parts = line.split(":", 3);
        if (parts.length == 3) {
          list.append("<li><strong>").append(parts[0]).append(":</strong> ").append(parts[1])
          .append(" <em>(").append(parts[2]).append(")</em></li>");
        }
      }
    } catch (IOException ioe) {
      System.out.println("Error generating messages: " + ioe);
      return "<p>No messages yet.</p>";
    }
    if (list.toString().equals("<ul>")) {
      return "<p>No messages yet.</p>";
    }
    return list.append("</ul>").toString();
  }

  private static boolean userExists(String username) {
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader("resources/users.txt"))) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        String[] parts = line.split(":");
        if (parts.length == 2 && parts[0].equals(username)) {
          return true;
        }
      }
    } catch (IOException ioe) {
      System.out.println("Error checking user: " + ioe);
    }
    return false;
  }

  private static void saveUser(String username, String password) {
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("resources/users.txt", true))) {
      bufferedWriter.write(username + ":" + password);
      bufferedWriter.newLine();
    } catch (IOException ioe) {
      System.out.println("Error saving user: " + ioe);
    }
  }
}
