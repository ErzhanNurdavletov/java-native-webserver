import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SessionService {

  private DataStore dataStore;
  private static final String SESSIONS_FILE = "sessions.txt";

  public SessionService(DataStore dataStore) {
    this.dataStore = dataStore;
  }

  public String createSession(String username) {
    String token = UUID.randomUUID().toString();
    dataStore.appendToFile(SESSIONS_FILE, token + ":" + username);
    return token;
  }

  public boolean isSessionValid(String token) {
    if (token == null || token.isEmpty()) return false;
    List<String> lines = dataStore.readAllLines(SESSIONS_FILE);
    for (String line : lines) {
      String[] parts = line.split(":");
      if (parts.length >= 2 && parts[0].equals(token)) {
        return true;
      }
    }
    return false;
  }

  public void deleteSession(String token) {
    if (token == null || token.isEmpty()) {
      return;
    }
    List<String> sessions = new ArrayList<>();
    List<String> lines = dataStore.readAllLines(SESSIONS_FILE);

    for (String line : lines) {
      if (!line.startsWith(token + ":")) {
        sessions.add(line);
      }
    }
    dataStore.writeAllLines(SESSIONS_FILE, sessions);
  }
}
