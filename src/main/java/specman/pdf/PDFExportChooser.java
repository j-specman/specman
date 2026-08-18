package specman.pdf;

import com.itextpdf.kernel.geom.PageSize;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.io.FilenameUtils;
import specman.Specman;
import specman.model.v002.PdfExportOptionsModel_V002;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.util.prefs.Preferences;

import static org.apache.commons.io.FilenameUtils.getBaseName;

public class PDFExportChooser extends JFileChooser {
  public static final String PDF_EXTENSION = ".pdf";
  public static final String PDF_PAGE_PORTRAIT_PREF = "pdf.page.portrait";
  public static final String PDF_PAGE_SIZE_PREF = "pdf.page.size";
  public static final String PDF_PAGING_PREF = "pdf.paging";
  public static final String PDF_DISPLAY_PREF = "pdf.display";

  JPanel pageOptions = new JPanel();
  JComboBox<String> pageSize = new JComboBox<>();
  JRadioButton portrait = new JRadioButton("Portrait");
  JRadioButton landscape = new JRadioButton("Landscape");
  JCheckBox paging = new JCheckBox("Paging");
  JCheckBox display = new JCheckBox("Display result");

  File currentModelFile = null;

  public PDFExportChooser() {
    setFileSelectionMode(JFileChooser.FILES_ONLY);
    addChoosableFileFilter(new FileNameExtensionFilter(PDF_EXTENSION, "pdf"));
    setAcceptAllFileFilterUsed(true);
    initPageOptions();
  }

  @Override
  protected JDialog createDialog(Component parent) throws HeadlessException {
    JDialog dialog = super.createDialog(parent);
    Container contentPane = dialog.getContentPane();
    contentPane.add(pageOptions, BorderLayout.NORTH);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    return dialog;
  }

  private void initPageOptions() {
    Preferences prefs = Preferences.userNodeForPackage(Specman.class);

    pageOptions = new JPanel();
    pageOptions.setLayout(new FormLayout("10px,78px,10px,fill:245px,10px", "10px, fill:28px, 28px, 28px, 10px"));
    pageOptions.add(new JLabel("Page size:"), CC.xy(2, 2));
    pageOptions.add(pageSize, CC.xy(4, 2));
    pageOptions.add(portrait, CC.xy(2, 3));
    pageOptions.add(landscape, CC.xy(4, 3));
    pageOptions.add(paging, CC.xy(2, 4));
    pageOptions.add(display, CC.xy(4, 4));
    addAvailablePageSizes(prefs);
    ButtonGroup orientiations = new ButtonGroup();
    orientiations.add(portrait);
    orientiations.add(landscape);
    setOrientation(prefs.getBoolean(PDF_PAGE_PORTRAIT_PREF, true));
    paging.setSelected(prefs.getBoolean(PDF_PAGING_PREF, false));
    display.setSelected(prefs.getBoolean(PDF_DISPLAY_PREF, true));
  }

  public void initFromModel(PdfExportOptionsModel_V002 pdfExportOptions, File currentModelFile) {
    if (pdfExportOptions != null) {
      setOrientation(pdfExportOptions.portrait);
      this.paging.setSelected(pdfExportOptions.paging);
      this.pageSize.setSelectedItem(pdfExportOptions.pageSize);
      this.currentModelFile = currentModelFile;
      File exportFile = guessBestFilenameAndDirectory(pdfExportOptions, currentModelFile);
      setSelectedFile(exportFile);
    }
  }

  /** Returns a reasonable PDF export file and directory by taking also the current
   * diagramm file into account and the diagramm file which the last PDF has been exported
   * for so that the export filename and location may reasonably move together with its
   * diagramm file. The guess is based on the following rules:
   * <ol>
   *   <li>If there is no information present about the current model file (model has not been saved yet)
   *   and the model file from the last PDF, we simply return the last PDF filename (if any).</li>
   *   <li>If there is only information present about the current model file (model has been saved in the
   *   meanwhile), and the last PDF export file has no path, we add the model files path to the PDF file.
   *   This will also keep compatibility with older versions of Specman where the former model filename
   *   was not persisted and the PDF filename was persisted without path.</li>
   *   <li>If the PDF filename had the same basename as the former model filename (i.e. filename without path and extension),
   *   we change the base name to the current model file's basename.</li>
   *   <li>If the PDF file was located in the same directory as the former model file, we change the
   *   directory to the current model file's directory.</li>
   * </ol>
   */
  private File guessBestFilenameAndDirectory(PdfExportOptionsModel_V002 pdfExportOptions, File currentModelFile) {
    File pdfFile = new File(pdfExportOptions.filename);
    if (pdfExportOptions.modelFilename != null && currentModelFile != null) {
      File formerModelFile = new File(pdfExportOptions.modelFilename);
      String pdfExtension = FilenameUtils.getExtension(pdfFile.getName());
      String pdfBasename = derivePDFBasename(pdfFile, formerModelFile, currentModelFile);
      String pdfDirectory = derivePDFDirectory(pdfFile, formerModelFile, currentModelFile);
      return new File(pdfDirectory, pdfBasename + "." + pdfExtension);
    }
    String pdfFilename = prependModelPath(pdfFile, currentModelFile);
    return new File(pdfFilename);
  }

  private String prependModelPath(File pdfFile, File currentModelFile) {
    if (currentModelFile != null && pdfFile.getParent() == null) {
      return new File(currentModelFile.getParent(), pdfFile.getName()).getAbsolutePath();
    }
    return pdfFile.getAbsolutePath();
  }

  private String derivePDFDirectory(File pdfFile, File formerModelFile, File currentModelFile) {
    String formerModelDirectory = formerModelFile.getParent();
    String pdfDirectory = pdfFile.getParent();
    if (pdfDirectory != null && pdfDirectory.equals(formerModelDirectory)) {
      return currentModelFile.getParent();
    }
    return pdfDirectory;
  }

  private String derivePDFBasename(File pdfFile, File formerModelFile, File currentModelFile) {
    String formerModelBasename = getBaseName(formerModelFile.getName());
    String pdfBasename = getBaseName(pdfFile.getName());
    if (pdfBasename.equals(formerModelBasename)) {
      return getBaseName(currentModelFile.getName());
    }
    return pdfBasename;
  }

  private void setOrientation(boolean portraitOrientation) {
    if (portraitOrientation) {
      portrait.setSelected(true);
    }
    else {
      landscape.setSelected(true);
    }
  }

  private void addAvailablePageSizes(Preferences prefs) {
    for (Field field : PageSize.class.getFields()) {
      if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
        pageSize.addItem(field.getName());
      }
    }
    pageSize.setSelectedItem(prefs.get(PDF_PAGE_SIZE_PREF, "A4"));
  }

  public PageSize getSelectedPageSize() {
    try {
      Field sizeField = PageSize.class.getField(pageSize.getSelectedItem().toString());
      return (PageSize)sizeField.get(null);
    }
    catch(Exception x) {
      x.printStackTrace();
    }
    return null;
  }

  public boolean getPaging() { return paging.isSelected(); }

  public boolean isPortrait() { return portrait.isSelected(); }

  public boolean displayResult() { return display.isSelected(); }

  public void safeUserPreferences() {
    Preferences prefs = Preferences.userNodeForPackage(Specman.class);
    prefs.put(PDF_PAGING_PREF, Boolean.toString(paging.isSelected()));
    prefs.put(PDF_PAGE_SIZE_PREF, pageSize.getSelectedItem().toString());
    prefs.put(PDF_PAGE_PORTRAIT_PREF, Boolean.toString(portrait.isSelected()));
    prefs.put(PDF_DISPLAY_PREF, Boolean.toString(display.isSelected()));
  }

  public int showSaveDialog(Component component, File diagrammDatei) {
    File exportDirectory = diagrammDatei != null
      ? diagrammDatei.getParentFile() : new File(".");
    setCurrentDirectory(exportDirectory);
    return showSaveDialog(component);
  }

  public PdfExportOptionsModel_V002 getExportOptions() {
    return new PdfExportOptionsModel_V002(
      getSelectedFile().getAbsolutePath(),
      currentModelFile != null ? currentModelFile.getAbsolutePath() : null,
      pageSize.getSelectedItem().toString(),
      portrait.isSelected(),
      paging.isSelected()
    );
  }

  @Override
  /** If the selected file is not an existing one and does not end with ".pdf", we append the extension ".pdf" */
  public File getSelectedFile() {
    File selectedFile = super.getSelectedFile();
    if (selectedFile != null && !selectedFile.exists()) {
      String filename = selectedFile.getAbsolutePath();
      if (!filename.toLowerCase().endsWith(PDF_EXTENSION)) {
        selectedFile = new File(filename + PDF_EXTENSION);
      }
    }
    return selectedFile;
  }

}