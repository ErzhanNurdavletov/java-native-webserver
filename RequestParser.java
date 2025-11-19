import java.util.HashMap;
import java.util.Map;

public class RequestParser {

  private String method;
  private String path;
  private Map<String, String> headers;
  private String body;

  public void parse(String request) {
    String[] lines = request.split("\r\n");
    if (lines.length == 0) {
      method = "";
      path = "";
      headers = new HashMap<>();
      body = "";
      return;
    }
    String[] requestLine = lines[0].split(" ");
    method = requestLine.length > 0 ? requestLine[0] : "";
    path = requestLine.length > 1 ? requestLine[1] : "";

    if (path.contains("?")) {
      path = path.substring(0, path.indexOf("?"));
    }
    headers = new HashMap<>();
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
    body = "";
    if ("POST".equals(method) && bodyStart > 0) {
      StringBuilder bodyBuffer = new StringBuilder();
      for (int i = bodyStart; i < lines.length; i++) {
        bodyBuffer.append(lines[i]);
        if (i < lines.length - 1) {
          bodyBuffer.append("\n");
        }
      }
      body = bodyBuffer.toString();
    }
  }

  public String getMethod() {
    return method;
  }

  public String getPath() {
    return path;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getBody() {
    return body;
  }

  public String getCookie(String name) {
    String cookieHeader = headers.get("cookie");
    if (cookieHeader == null) return null;
    String[] cookies = cookieHeader.split("; ");
    for (String cookie : cookies) {
      if (cookie.startsWith(name + "=")) {
        return cookie.substring(name.length() + 1);
      }
    }
    return null;
  }

  public Map<String, String> parseFormData() {
    Map<String, String> data = new HashMap<>();
    if (body != null) {
      String[] pairs = body.split("&");
      for (String pair : pairs) {
        String[] kv = pair.split("=", 2);
        if (kv.length == 2) {
          data.put(kv[0], kv[1].replace("+", " "));
        }
      }
    }
    return data;
  }
}
