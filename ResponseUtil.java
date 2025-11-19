import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class ResponseUtil {

  public void sendResponse(PrintWriter printWriter, OutputStream outputStream, int code, String statusText, String contentType, byte[] data) throws IOException {
    printWriter.println("HTTP/1.1 " + code + " " + statusText);
    printWriter.println("Server: Java HTTP Server");
    printWriter.println("Content-Type: " + contentType);
    printWriter.println("Content-Length: " + data.length);
    printWriter.println();
    printWriter.flush();
    BufferedOutputStream dataOut = new BufferedOutputStream(outputStream);
    dataOut.write(data, 0, data.length);
    dataOut.flush();
  }

  public void sendRedirect(PrintWriter printWriter, String location, String cookieHeader) {
    printWriter.println("HTTP/1.1 302 Found");
    printWriter.println("Location: " + location);
    if (cookieHeader != null) {
      printWriter.println(cookieHeader);
    }
    printWriter.println();
    printWriter.flush();
  }
}
