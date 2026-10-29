package specman.link;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import net.atlanticbb.tantlinger.ui.UIUtils;
import specman.i18n.IButton;
import specman.i18n.IHeaderPanel;
import specman.i18n.ILabel;
import specman.view.AbstractSchrittView;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.Serial;
import java.util.function.Consumer;

import static specman.i18n.Messages.i18n;

/** The content of the {@link AliasDialog}, in which the user is asked for an alias for the file a link points to.
 * While typing, the panel shows the resulting link and what is wrong with the alias, if anything.
 * Being a plain panel, it can be created and operated without displaying a dialog. */
public class AliasPanel extends JPanel {
  @Serial
  private static final long serialVersionUID = 1L;

  public static final String ALIAS_FIELD = "link.alias.field";
  public static final String PREVIEW_FIELD = "link.alias.preview";
  public static final String PROBLEM_LABEL = "link.alias.problem";
  public static final String ALL_ALIASES_BUTTON = "link.alias.all";
  public static final String OK_BUTTON = "link.alias.ok";
  public static final String CANCEL_BUTTON = "link.alias.cancel";

  private static final Icon ICON = UIUtils.getIcon("resources/images/x32/", "link3.png");
  private static final Color PROBLEM_COLOR = new Color(192, 0, 0);
  private static final Color LINK_COLOR = new Color(0, 102, 204);
  private static final int ALIAS_FIELD_COLUMNS = 20;
  private static final int PREVIEW_FIELD_COLUMNS = 40;
  // Keeps the row of the problem label at its height while there is no problem, so the dialog doesn't have to grow
  private static final String NO_PROBLEM_TEXT = " ";

  private final File file;
  private final AbstractSchrittView step;
  private final Consumer<String> aliasAccepted;
  private final JTextField aliasField = new JTextField(ALIAS_FIELD_COLUMNS);
  private final JTextField previewField = new JTextField(PREVIEW_FIELD_COLUMNS);
  private final JLabel problemLabel = new JLabel(NO_PROBLEM_TEXT);
  private final JButton allAliases = new IButton("dialog.link.alias.all");
  private final JButton ok = new IButton("button.ok");
  private final JButton cancel = new IButton("button.cancel");
  private Runnable closeAction = () -> { };

  /** @param aliasAccepted called with the alias, possibly empty for "no alias", when the user confirms, before the panel closes */
  public AliasPanel(File file, AbstractSchrittView step, Consumer<String> aliasAccepted) {
    super(new FormLayout("10px, right:default, 8px, fill:default:grow, 10px",
      "pref, 10px, pref, 4px, pref, 4px, pref, 10px, pref, 10px"));
    this.file = file;
    this.step = step;
    this.aliasAccepted = aliasAccepted;
    aliasField.setName(ALIAS_FIELD);
    ((AbstractDocument) aliasField.getDocument()).setDocumentFilter(new AliasInputFilter());
    previewField.setName(PREVIEW_FIELD);
    previewField.setEditable(false);
    makeLookLikeLabel(previewField);
    problemLabel.setName(PROBLEM_LABEL);
    problemLabel.setForeground(PROBLEM_COLOR);
    allAliases.setName(ALL_ALIASES_BUTTON);
    makeLookLikeWebLink(allAliases);
    ok.setName(OK_BUTTON);
    ok.addActionListener(e -> confirm());
    cancel.setName(CANCEL_BUTTON);
    cancel.addActionListener(e -> closeAction.run());
    add(new IHeaderPanel("dialog.link.alias.title", "dialog.link.alias.description", ICON), CC.xyw(1, 1, 5));
    add(new ILabel("dialog.link.alias.label"), CC.xy(2, 3));
    add(buildAliasPanel(), CC.xy(4, 3, "left, center"));
    add(new ILabel("dialog.link.alias.preview.label"), CC.xy(2, 5));
    add(previewField, CC.xy(4, 5));
    add(problemLabel, CC.xy(4, 7));
    add(buildButtonPanel(), CC.xyw(2, 9, 3));
    aliasField.getDocument().addDocumentListener(new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e) { refresh(); }
      @Override public void removeUpdate(DocumentEvent e) { refresh(); }
      @Override public void changedUpdate(DocumentEvent e) { refresh(); }
    });
    registerEscapeAsCancel();
    refresh();
  }

  /** Defines what to do when the panel is done, i.e. after the user confirmed or cancelled. */
  public void onClose(Runnable closeAction) {
    this.closeAction = closeAction;
  }

  /** Once the panel is part of a window, Enter confirms and the cursor is where the user types the alias. */
  @Override
  public void addNotify() {
    super.addNotify();
    getRootPane().setDefaultButton(ok);
    aliasField.requestFocusInWindow();
  }

  private void makeLookLikeLabel(JTextField field) {
    field.setOpaque(false);
    // A plain Color, not the UIResource of the look and feel, which would paint a read-only field gray
    field.setBackground(new Color(getBackground().getRGB()));
    field.setBorder(BorderFactory.createEmptyBorder());
  }

  private void refresh() {
    String alias = aliasField.getText();
    String problemKey = LinkAliases.problemKeyFor(file, alias);
    ok.setEnabled(problemKey == null);
    problemLabel.setText(problemKey == null ? NO_PROBLEM_TEXT : i18n(problemKey));
    previewField.setText(problemKey == null ? SpecmanLink.of(file, alias, step) : "");
  }

  private void confirm() {
    aliasAccepted.accept(aliasField.getText());
    closeAction.run();
  }

  private JPanel buildAliasPanel() {
    JPanel panel = new JPanel(new FormLayout("default, 8px, default", "pref"));
    panel.add(aliasField, CC.xy(1, 1));
    panel.add(allAliases, CC.xy(3, 1));
    return panel;
  }

  private static void makeLookLikeWebLink(JButton button) {
    button.setBorder(BorderFactory.createEmptyBorder());
    button.setContentAreaFilled(false);
    button.setFocusPainted(false);
    button.setForeground(LINK_COLOR);
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
  }

  private JPanel buildButtonPanel() {
    JPanel panel = new JPanel(new FormLayout("fill:default:grow, default, 6px, default", "pref"));
    panel.add(ok, CC.xy(2, 1));
    panel.add(cancel, CC.xy(4, 1));
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
}
