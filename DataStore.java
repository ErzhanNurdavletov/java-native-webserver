import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataStore {

  public String readFileContent(String fileName) {
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
      StringBuilder content = new StringBuilder();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        content.append(line).append("\n");
      }
      return content.toString();
    } catch (IOException ioe) {
      System.out.println("Error reading file ");
      return "";
    }
  }

  public byte[] readFileBytes(String fileName) {
    try {
      File file = new File(fileName);
      if (file.exists()) {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
          fis.read(bytes);
        }
        return bytes;
      }
    } catch (IOException ioe) {
      System.out.println("Error reading bytes ");
    }
    return null;
  }

  public void appendToFile(String fileName, String line) {
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName, true))) {
      bufferedWriter.write(line);
      bufferedWriter.newLine();
    } catch (IOException ioe) {
      System.out.println("Error appending to file ");
    }
  }

  public List<String> readAllLines(String fileName) {
    List<String> lines = new ArrayList<>();
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        lines.add(line);
      }
    } catch (IOException ioe) {
      System.out.println("Error reading lines from file ");
    }
    return lines;
  }

  public void writeAllLines(String fileName, List<String> lines) {
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName))) {
      for (String line : lines) {
        bufferedWriter.write(line);
        bufferedWriter.newLine();
      }
    } catch (IOException ioe) {
      System.out.println("Error writing lines to file ");
    }
  }
}
