package specman.link;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import net.atlanticbb.tantlinger.ui.UIUtils;
import specman.i18n.IButton;
import specman.i18n.IHeaderPanel;
import specman.i18n.ILabel;
import specman.view.AbstractSchrittView;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.Serial;

/** The content of the {@link AliasDialog}, in which the user is asked for an alias for the file a link points to.
 * Being a plain panel, it can be created and operated without displaying a dialog. */
public class AliasPanel extends JPanel {
  @Serial
  private static final long serialVersionUID = 1L;

  public static final String ALIAS_FIELD = "link.alias.field";
  public static final String PREVIEW_FIELD = "link.alias.preview";
  public static final String OK_BUTTON = "link.alias.ok";
  public static final String CANCEL_BUTTON = "link.alias.cancel";

  private static final Icon ICON = UIUtils.getIcon("resources/images/x32/", "link3.png");
  private static final int ALIAS_FIELD_COLUMNS = 20;
  private static final int PREVIEW_FIELD_COLUMNS = 40;

  private final JTextField aliasField = new JTextField(ALIAS_FIELD_COLUMNS);
  private final JTextField previewField = new JTextField(PREVIEW_FIELD_COLUMNS);
  private final JButton ok = new IButton("button.ok");
  private final JButton cancel = new IButton("button.cancel");
  private Runnable closeAction = () -> { };

  public AliasPanel(File file, AbstractSchrittView step) {
    super(new FormLayout("10px, right:default, 8px, fill:default:grow, 10px",
      "pref, 10px, pref, 4px, pref, 10px, pref, 10px"));
    aliasField.setName(ALIAS_FIELD);
    aliasField.setEnabled(false);
    previewField.setName(PREVIEW_FIELD);
    previewField.setEditable(false);
    previewField.setText(SpecmanLink.of(file, step));
    ok.setName(OK_BUTTON);
    ok.setEnabled(false);
    cancel.setName(CANCEL_BUTTON);
    cancel.addActionListener(e -> closeAction.run());
    add(new IHeaderPanel("dialog.link.alias.title", "dialog.link.alias.description", ICON), CC.xyw(1, 1, 5));
    add(new ILabel("dialog.link.alias.label"), CC.xy(2, 3));
    add(aliasField, CC.xy(4, 3, "left, center"));
    add(new ILabel("dialog.link.alias.preview.label"), CC.xy(2, 5));
    add(previewField, CC.xy(4, 5));
    add(buildButtonPanel(), CC.xyw(2, 7, 3));
    registerEscapeAsCancel();
  }

  /** Defines what to do when the panel is done, i.e. after the user confirmed or cancelled. */
  public void onClose(Runnable closeAction) {
    this.closeAction = closeAction;
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
