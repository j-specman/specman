package classman.editor;

import java.util.List;
import java.util.Locale;

/** The file formats in which a diagram can be exported. */
enum ExportFormat {
  PNG("png", List.of("png")),
  JPEG("jpg", List.of("jpg", "jpeg")),
  SVG("svg", List.of("svg")),
  CLASSMAN("cld", List.of("cld"));

  /** The extension of the files which the editor writes. */
  final String extension;
  final List<String> extensions;

  ExportFormat(String extension, List<String> extensions) {
    this.extension = extension;
    this.extensions = extensions;
  }

  /** @return the key of the name of the format in the texts of the editor */
  String textKey() { return "export.format." + name().toLowerCase(Locale.ROOT); }

  /** @return the format which the extension of the file name tells, null if the name has none of a format */
  static ExportFormat ofFileName(String fileName) {
    int dot = fileName.lastIndexOf('.');
    if (dot < 0) {
      return null;
    }
    String fileExtension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    for (ExportFormat format : values()) {
      if (format.extensions.contains(fileExtension)) {
        return format;
      }
    }
    return null;
  }
}
