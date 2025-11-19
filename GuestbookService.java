import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GuestbookService {

  private static final String MESSAGES_FILE = "messages.txt";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private DataStore dataStore;

  public GuestbookService(DataStore dataStore) {
    this.dataStore = dataStore;
  }

  public void saveMessage(String name, String messageText) {
    String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
    String line = name + ":" + messageText + ":" + timestamp;
    dataStore.appendToFile(MESSAGES_FILE, line);
  }

  public String generateMessagesList() {
    StringBuilder list = new StringBuilder("<ul>");
    List<String> lines = dataStore.readAllLines(MESSAGES_FILE);

    for (String line : lines) {
      String[] parts = line.split(":", 3);
      if (parts.length == 3) {
        list.append("<li><strong>").append(parts[0]).append(":</strong> ").append(parts[1])
        .append(" <em>(").append(parts[2]).append(")</em></li>");
      }
    }

    if (list.toString().equals("<ul>")) {
      return "<p>Пока нет сообщений.</p>";
    }
    return list.append("</ul>").toString();
  }
}
