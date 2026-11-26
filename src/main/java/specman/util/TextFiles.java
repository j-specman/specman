package specman.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Reading text files the way Specman wants their text. */
public final class TextFiles {
  private static final String BYTE_ORDER_MARK = String.valueOf((char) 0xFEFF);

  private TextFiles() {}

  /** @return the text of the UTF-8 file with the line breaks Specman uses and without the byte order mark some editors
   * put at the start */
  public static String read(File file) throws IOException {
    String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    return content
      .replace(BYTE_ORDER_MARK, "")
      .replace("\r\n", "\n")
      .replace('\r', '\n');
  }
}
