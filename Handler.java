import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.Socket;

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
            if (path.contains("?")) path = path.substring(0, path.indexOf("?"));

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
                StringBuilder bodySb = new StringBuilder();
                for (int i = bodyStart; i < lines.length; i++) {
                    bodySb.append(lines[i]);
                }
                body = bodySb.toString();
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
    private static void sendResponse(PrintWriter pw, OutputStream out, int code, String text, String type, byte[] data) throws IOException {
        pw.println("HTTP/1.1 " + code + " " + text);
        pw.println("Server: Java HTTP Server from Intern Labs 7.0 - Java Backend Developer");
        pw.println("Content-Type: " + type);
        pw.println("Content-Length: " + data.length);
        pw.println();
        pw.flush();
        BufferedOutputStream dataOut = new BufferedOutputStream(out);
        dataOut.write(data, 0, data.length);
        dataOut.flush();
    }

    private static String readFileContent(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            System.out.println("Error reading file " + path + ": " + e);
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
        } catch (IOException e) {
            System.out.println("Error reading bytes " + path + ": " + e);
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
                    data.put(kv[0], kv[1].replace("+", " ")); // Простая декодировка
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
        try (BufferedReader br = new BufferedReader(new FileReader("resources/users.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(password)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Error validating user: " + e);
        }
        return false;
    }

    private static void saveSession(String token, String username, long expiration) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("resources/sessions.txt", true))) {
            bw.write(token + ":" + username + ":" + expiration);
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error saving session: " + e);
        }
    }

    private static boolean isSessionValid(String token) {
        if (token == null) return false;
        try (BufferedReader br = new BufferedReader(new FileReader("resources/sessions.txt"))) {
            String line;
            long now = System.currentTimeMillis();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 3 && parts[0].equals(token) && Long.parseLong(parts[2]) > now) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Error checking session: " + e);
        }
        return false;
    }

    private static void deleteSession(String token) {
        if (token == null) return;
        List<String> sessions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("resources/sessions.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith(token + ":")) {
                    sessions.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading sessions: " + e);
            return;
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("resources/sessions.txt"))) {
            for (String session : sessions) {
                bw.write(session);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error writing sessions: " + e);
        }
    }

    private static void saveMessage(String username, String message, String timestamp) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("resources/messages.txt", true))) {
            bw.write(username + ":" + message + ":" + timestamp);
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error saving message: " + e);
        }
    }

    private static String generateMessagesList() {
        StringBuilder list = new StringBuilder("<ul>");
        try (BufferedReader br = new BufferedReader(new FileReader("resources/messages.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":", 3);
                if (parts.length == 3) {
                    list.append("<li><strong>").append(parts[0]).append(":</strong> ").append(parts[1])
                        .append(" <em>(").append(parts[2]).append(")</em></li>");
                }
            }
        } catch (IOException e) {
            System.out.println("Error generating messages: " + e);
            return "<p>No messages yet.</p>";
        }
        if (list.toString().equals("<ul>")) {
            return "<p>No messages yet.</p>";
        }
        return list.append("</ul>").toString();
    }
}
