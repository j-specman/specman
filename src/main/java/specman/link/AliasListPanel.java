package specman.link;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.i18n.IButton;
import specman.i18n.IHeaderPanel;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;

import static specman.i18n.Messages.i18n;

/** The content of the {@link AliasListDialog}, which shows all aliases the user assigned to files so far.
 * Being a plain panel, it can be created and operated without displaying a dialog. */
public class AliasListPanel extends JPanel {
  @Serial
  private static final long serialVersionUID = 1L;

  public static final String TABLE = "link.aliases.table";
  public static final String CLOSE_BUTTON = "link.aliases.close";
  public static final int ALIAS_COLUMN = 0;
  public static final int FILE_COLUMN = 1;

  private static final int VISIBLE_ROWS = 8;
  private static final int VISIBLE_CHARACTERS = 70;

  private final JTable table = new JTable(createModel());
  private final JButton close = new IButton("button.close");
  private Runnable closeAction = () -> { };

  public AliasListPanel() {
    super(new FormLayout("10px, fill:default:grow, 10px", "pref, 10px, fill:default:grow, 10px, pref, 10px"));
    table.setName(TABLE);
    table.setFillsViewportHeight(true);
    int characterWidth = table.getFontMetrics(table.getFont()).charWidth('m');
    table.setPreferredScrollableViewportSize(
      new Dimension(characterWidth * VISIBLE_CHARACTERS, table.getRowHeight() * VISIBLE_ROWS));
    table.getColumnModel().getColumn(ALIAS_COLUMN).setPreferredWidth(characterWidth * LinkAliases.MAX_ALIAS_LENGTH);
    table.getColumnModel().getColumn(FILE_COLUMN).setPreferredWidth(characterWidth * (VISIBLE_CHARACTERS - LinkAliases.MAX_ALIAS_LENGTH));
    close.setName(CLOSE_BUTTON);
    close.addActionListener(e -> closeAction.run());
    add(new IHeaderPanel("dialog.link.aliases.title", "dialog.link.aliases.description", AliasPanel.ICON), CC.xyw(1, 1, 3));
    add(new JScrollPane(table), CC.xy(2, 3));
    add(buildButtonPanel(), CC.xy(2, 5));
    registerEscapeAsClose();
  }

  /** Defines what to do when the panel is done, i.e. after the user closed it. */
  public void onClose(Runnable closeAction) {
    this.closeAction = closeAction;
  }

  /** Once the panel is part of a window, Enter closes it. */
  @Override
  public void addNotify() {
    super.addNotify();
    getRootPane().setDefaultButton(close);
  }

  private static DefaultTableModel createModel() {
    DefaultTableModel model = new DefaultTableModel(
      new String[] {i18n("dialog.link.aliases.column.alias"), i18n("dialog.link.aliases.column.file")}, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    for (LinkAliases.Entry entry : LinkAliases.entries()) {
      model.addRow(new String[] {entry.alias(), entry.file().getPath()});
    }
    return model;
  }

  private JPanel buildButtonPanel() {
    JPanel panel = new JPanel(new FormLayout("fill:default:grow, default", "pref"));
    panel.add(close, CC.xy(2, 1));
    return panel;
  }

  private void registerEscapeAsClose() {
    String actionKey = "close";
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionKey);
    getActionMap().put(actionKey, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        closeAction.run();
      }
    });
  }
}
