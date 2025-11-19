import java.util.List;

public class UserService {

  private DataStore dataStore;
  private static final String USERS_FILE = "users.txt";

  public UserService(DataStore dataStore) {
    this.dataStore = dataStore;
  }

  public boolean validateUser(String username, String password) {
    List<String> lines = dataStore.readAllLines(USERS_FILE);
    for (String line : lines) {
      String[] parts = line.split(":");
      if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(password)) {
        return true;
      }
    }
    return false;
  }

  public boolean userExists(String username) {
    List<String> lines = dataStore.readAllLines(USERS_FILE);
    for (String line : lines) {
      String[] parts = line.split(":");
      if (parts.length == 2 && parts[0].equals(username)) {
        return true;
      }
    }
    return false;
  }

  public void saveUser(String username, String password) {
    dataStore.appendToFile(USERS_FILE, username + ":" + password);
  }
}
