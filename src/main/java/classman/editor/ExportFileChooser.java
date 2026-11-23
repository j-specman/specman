package classman.editor;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/** The dialog in which the user chooses the file to export the diagram to. The file type selector chooses the format,
 * unless the user types a name with the extension of another format. The directory and the format which were chosen
 * last are offered the next time, as long as the application runs. */
class ExportFileChooser extends JFileChooser {
  private static File lastDirectory;
  private static ExportFormat lastFormat = ExportFormat.PNG;

  private final EditorTexts texts;
  private final Map<FileFilter, ExportFormat> formats = new LinkedHashMap<>();

  ExportFileChooser(EditorTexts texts) {
    super(lastDirectory);
    this.texts = texts;
    setDialogTitle(texts.get("export.title"));
    setAcceptAllFileFilterUsed(false);
    for (ExportFormat format : ExportFormat.values()) {
      FileNameExtensionFilter filter = new FileNameExtensionFilter(
        texts.get(format.textKey()) + " (*." + format.extension + ")", format.extensions.toArray(new String[0]));
      formats.put(filter, format);
      addChoosableFileFilter(filter);
      if (format == lastFormat) {
        setFileFilter(filter);
      }
    }
  }

  /** @return the file which the user chose, with the extension of its format, null if the user gave up */
  File choose(Component parent) {
    if (showSaveDialog(parent) != APPROVE_OPTION) {
      return null;
    }
    lastDirectory = getCurrentDirectory();
    lastFormat = format();
    return getSelectedFile();
  }

  /** @return the format of the file which the user chose */
  ExportFormat format() {
    ExportFormat byName = ExportFormat.ofFileName(getSelectedFile().getName());
    return byName != null ? byName : formats.get(getFileFilter());
  }

  @Override
  public void approveSelection() {
    File chosen = getSelectedFile();
    if (chosen == null) {
      return;
    }
    if (ExportFormat.ofFileName(chosen.getName()) == null) {
      chosen = new File(chosen.getParentFile(), chosen.getName() + "." + formats.get(getFileFilter()).extension);
      setSelectedFile(chosen);
    }
    if (chosen.exists() && JOptionPane.showConfirmDialog(this, chosen.getName() + "\n" + texts.get("export.overwrite"),
      texts.get("export.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
      return;
    }
    super.approveSelection();
  }
}
