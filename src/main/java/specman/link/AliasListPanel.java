package specman.link;

import com.formdev.flatlaf.util.UIScale;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.i18n.IButton;
import specman.i18n.IFileNameExtensionFilter;
import specman.i18n.IHeaderPanel;
import specman.link.LinkAliases.Entry;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.AbstractDocument;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static specman.i18n.Messages.i18n;

/** The content of the {@link AliasListDialog}, in which the user maintains all aliases assigned to files so far:
 * Change an alias, delete a row, or choose another file for a row, typically because the file moved. Files which
 * don't exist anymore are marked. Nothing is stored until the user saves, which is only possible while the aliases
 * can be stored together, see {@link LinkAliases#problemKeyFor(List)}.
 * Being a plain panel, it can be created and operated without displaying a dialog. */
public class AliasListPanel extends JPanel {
  @Serial
  private static final long serialVersionUID = 1L;

  public static final String TABLE = "link.aliases.table";
  public static final String PROBLEM_LABEL = "link.aliases.problem";
  public static final String DELETE_BUTTON = "link.aliases.delete";
  public static final String CHOOSE_FILE_BUTTON = "link.aliases.choose.file";
  public static final String SAVE_BUTTON = "link.aliases.save";
  public static final String CANCEL_BUTTON = "link.aliases.cancel";
  public static final int ALIAS_COLUMN = 0;
  public static final int FILE_COLUMN = 1;

  private static final int VISIBLE_ROWS = 8;
  private static final int VISIBLE_CHARACTERS = 46;
  // Typical aliases are short, and the width is measured in the widest character
  private static final int ALIAS_COLUMN_CHARACTERS = 9;
  private static final int HEADER_ICON_AND_MARGINS_WIDTH = 40;
  private static final Color PROBLEM_COLOR = AliasPanel.PROBLEM_COLOR;
  // Keeps the row of the problem label at its height while there is no problem, so the dialog doesn't have to grow
  private static final String NO_PROBLEM_TEXT = " ";

  private final AliasTableModel model = new AliasTableModel(LinkAliases.entries());
  private final JTable table = new JTable(model);
  private final JLabel problemLabel = new JLabel(NO_PROBLEM_TEXT);
  private final JButton delete = new IButton("button.delete");
  private final JButton chooseFile = new IButton("button.choose");
  private final JButton save = new IButton("button.save");
  private final JButton cancel = new IButton("button.cancel");
  private Runnable closeAction = () -> { };
  private UnaryOperator<File> fileChooser = this::askUserForFile;

  public AliasListPanel() {
    super(new FormLayout("10px, fill:default:grow, 10px",
      "pref, 10px, fill:default:grow, 4px, pref, 10px, pref, 10px"));
    configureTable();
    problemLabel.setName(PROBLEM_LABEL);
    problemLabel.setForeground(PROBLEM_COLOR);
    delete.setName(DELETE_BUTTON);
    delete.addActionListener(e -> deleteSelectedRows());
    chooseFile.setName(CHOOSE_FILE_BUTTON);
    chooseFile.addActionListener(e -> chooseFileForSelectedRow());
    save.setName(SAVE_BUTTON);
    save.addActionListener(e -> save());
    cancel.setName(CANCEL_BUTTON);
    cancel.addActionListener(e -> closeAction.run());
    add(new IHeaderPanel("dialog.link.aliases.title", "dialog.link.aliases.description", AliasPanel.ICON,
      descriptionWidth()), CC.xyw(1, 1, 3));
    add(new JScrollPane(table), CC.xy(2, 3));
    add(problemLabel, CC.xy(2, 5));
    add(buildButtonPanel(), CC.xy(2, 7));
    table.getSelectionModel().addListSelectionListener(e -> refresh());
    model.addTableModelListener(e -> refresh());
    registerEscapeAsCancel();
    refresh();
  }

  /** Defines what to do when the panel is done, i.e. after the user saved or cancelled. */
  public void onClose(Runnable closeAction) {
    this.closeAction = closeAction;
  }

  /** Replaces the question to the user which file a row shall point to. The function gets the row's current file
   * and returns the chosen one, null if the user doesn't choose any. Allows tests to do without a file chooser. */
  public void setFileChooser(UnaryOperator<File> fileChooser) {
    this.fileChooser = fileChooser;
  }

  /** Once the panel is part of a window, Enter saves. */
  @Override
  public void addNotify() {
    super.addNotify();
    getRootPane().setDefaultButton(save);
  }

  private void configureTable() {
    table.setName(TABLE);
    table.setFillsViewportHeight(true);
    // Leaving the table by clicking a button must not lose what the user just typed into a cell
    table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    int characterWidth = table.getFontMetrics(table.getFont()).charWidth('m');
    table.setPreferredScrollableViewportSize(
      new Dimension(characterWidth * VISIBLE_CHARACTERS, table.getRowHeight() * VISIBLE_ROWS));
    table.getColumnModel().getColumn(ALIAS_COLUMN).setPreferredWidth(characterWidth * ALIAS_COLUMN_CHARACTERS);
    table.getColumnModel().getColumn(FILE_COLUMN).setPreferredWidth(characterWidth * (VISIBLE_CHARACTERS - ALIAS_COLUMN_CHARACTERS));
    table.getColumnModel().getColumn(ALIAS_COLUMN).setCellEditor(new DefaultCellEditor(createAliasField()));
    table.getColumnModel().getColumn(FILE_COLUMN).setCellRenderer(new FileRenderer());
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
          chooseFileOfClickedRow(e.getPoint());
        }
      }
    });
  }

  /** A double click on a file chooses another file for the row, like a double click on an alias edits it. */
  private void chooseFileOfClickedRow(Point point) {
    int row = table.rowAtPoint(point);
    int column = table.columnAtPoint(point);
    if (row >= 0 && column >= 0 && table.convertColumnIndexToModel(column) == FILE_COLUMN) {
      table.setRowSelectionInterval(row, row);
      chooseFileForSelectedRow();
    }
  }

  /** The description wraps at the width of the table less what the header needs for its icon and margins. The
   * header takes HTML pixels, which are scaled with the display, while the table width is in screen pixels. */
  private int descriptionWidth() {
    return UIScale.unscale(table.getPreferredScrollableViewportSize().width) - HEADER_ICON_AND_MARGINS_WIDTH;
  }

  private static JTextField createAliasField() {
    JTextField field = new JTextField();
    ((AbstractDocument) field.getDocument()).setDocumentFilter(new AliasInputFilter());
    return field;
  }

  private void refresh() {
    String problemKey = LinkAliases.problemKeyFor(model.entries());
    problemLabel.setText(problemKey == null ? NO_PROBLEM_TEXT : i18n(problemKey));
    save.setEnabled(problemKey == null);
    delete.setEnabled(table.getSelectedRowCount() > 0);
    chooseFile.setEnabled(table.getSelectedRowCount() == 1);
  }

  private void deleteSelectedRows() {
    stopEditing();
    int[] rows = table.getSelectedRows();
    for (int i = rows.length - 1; i >= 0; i--) {
      model.removeRow(table.convertRowIndexToModel(rows[i]));
    }
  }

  private void chooseFileForSelectedRow() {
    stopEditing();
    int row = table.convertRowIndexToModel(table.getSelectedRow());
    File chosen = fileChooser.apply(model.fileAt(row));
    if (chosen != null) {
      model.setFile(row, chosen);
    }
  }

  private File askUserForFile(File current) {
    JFileChooser chooser = new JFileChooser(current.getParentFile());
    chooser.setSelectedFile(current);
    chooser.setDialogTitle(i18n("dialog.link.aliases.choose.file"));
    chooser.setFileFilter(new IFileNameExtensionFilter("msg.filter.diagrams", "nsd"));
    return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
  }

  private void save() {
    stopEditing();
    LinkAliases.replaceAll(model.entries());
    closeAction.run();
  }

  private void stopEditing() {
    if (table.isEditing()) {
      table.getCellEditor().stopCellEditing();
    }
  }

  private JPanel buildButtonPanel() {
    JPanel panel = new JPanel(new FormLayout("default, 6px, default, fill:default:grow, default, 6px, default", "pref"));
    panel.add(delete, CC.xy(1, 1));
    panel.add(chooseFile, CC.xy(3, 1));
    panel.add(save, CC.xy(5, 1));
    panel.add(cancel, CC.xy(7, 1));
    return panel;
  }

  private void registerEscapeAsCancel() {
    String actionKey = "cancel";
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionKey);
    getActionMap().put(actionKey, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        closeAction.run();
      }
    });
  }

  /** The rows being edited. The file column shows the path, the alias column is the only one the user edits directly. */
  private static class AliasTableModel extends AbstractTableModel {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<Entry> rows;

    AliasTableModel(List<Entry> entries) {
      rows = new ArrayList<>(entries);
    }

    List<Entry> entries() {
      return List.copyOf(rows);
    }

    File fileAt(int row) {
      return rows.get(row).file();
    }

    void setFile(int row, File file) {
      rows.set(row, new Entry(file, rows.get(row).alias()));
      fireTableRowsUpdated(row, row);
    }

    void removeRow(int row) {
      rows.remove(row);
      fireTableRowsDeleted(row, row);
    }

    @Override
    public int getRowCount() {
      return rows.size();
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public String getColumnName(int column) {
      return i18n(column == ALIAS_COLUMN ? "dialog.link.aliases.column.alias" : "dialog.link.aliases.column.file");
    }

    @Override
    public Object getValueAt(int row, int column) {
      return column == ALIAS_COLUMN ? rows.get(row).alias() : rows.get(row).file().getPath();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      return column == ALIAS_COLUMN;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
      rows.set(row, new Entry(rows.get(row).file(), (String) value));
      fireTableCellUpdated(row, column);
    }
  }

  /** Shows the path of a file which doesn't exist anymore in red, followed by a note. */
  private static class FileRenderer extends DefaultTableCellRenderer {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      boolean missing = !new File((String) value).exists();
      if (missing) {
        setText(value + "  " + i18n("dialog.link.aliases.file.missing"));
      }
      // The renderer remembers a foreground set here as the one of all following unselected cells, so each cell sets it
      setForeground(isSelected ? table.getSelectionForeground() : missing ? PROBLEM_COLOR : table.getForeground());
      return component;
    }
  }
}
