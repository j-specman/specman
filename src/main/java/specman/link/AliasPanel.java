package specman.link;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.graphics.IconReader;
import specman.graphics.SpecmanIcons;
import specman.graphics.Styles;
import specman.i18n.IButton;
import specman.i18n.IHeaderPanel;
import specman.i18n.ILabel;
import specman.link.suggester.AliasSuggester;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
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
import java.util.function.Function;

import static specman.i18n.Messages.i18n;

/** The content of the {@link AliasDialog}, in which the user is asked how a link names the file it points to: by an alias,
 * which the user types, or by one of the paths which the buttons offer, see {@link LinkLocation}. While typing, the panel
 * shows the resulting link and what is wrong with the alias, if anything.
 * Being a plain panel, it can be created and operated without displaying a dialog.
 * <p>
 * A button changes the link once, and can be clicked at any time. It takes away what has been typed in the alias field.
 * Typing again goes back to an alias, and an empty alias field means the absolute path of the file.
 * <p>
 * What differs between the kinds of links is the text of the dialog, the link shown as the result and the paths on offer,
 * which the subclasses pass to the constructor: {@link StepLinkAliasPanel} for links to a step, and the panel for links to
 * a Classman diagram. */
public abstract class AliasPanel extends JPanel {
  @Serial
  private static final long serialVersionUID = 1L;

  public static final String ALIAS_FIELD = "link.alias.field";
  public static final String PREVIEW_FIELD = "link.alias.preview";
  public static final String PROBLEM_LABEL = "link.alias.problem";
  public static final String ALL_ALIASES_BUTTON = "link.alias.all";
  public static final String ABSOLUTE_BUTTON = "link.alias.absolute";
  public static final String RELATIVE_BUTTON = "link.alias.relative";
  public static final String LOCAL_BUTTON = "link.alias.local";
  public static final String OK_BUTTON = "link.alias.ok";
  public static final String CANCEL_BUTTON = "link.alias.cancel";

  static final Icon ICON = IconReader.readSvgIcon(SpecmanIcons.LINK, SpecmanIcons.SIZE_DIALOG);
  private static final Color LINK_COLOR = new Color(0, 102, 204);
  private static final int ALIAS_FIELD_COLUMNS = 20;
  private static final int PREVIEW_FIELD_COLUMNS = 40;
  // Keeps the row of the problem label at its height while there is no problem, so the dialog doesn't have to grow
  private static final String NO_PROBLEM_TEXT = " ";

  private final File file;
  private final String titleKey;
  private final Function<LinkLocation, String> linkOf;
  private final Consumer<LinkLocation> locationAccepted;
  private final JTextField aliasField = new JTextField(ALIAS_FIELD_COLUMNS);
  private final JTextField previewField = new JTextField(PREVIEW_FIELD_COLUMNS);
  private final JLabel problemLabel = new JLabel(NO_PROBLEM_TEXT);
  private final JButton allAliases = new IButton("dialog.link.alias.all");
  private final JButton ok = new IButton("button.ok");
  private final JButton cancel = new IButton("button.cancel");
  /** What the user chose with one of the buttons, null while the alias field decides. */
  private LinkLocation chosenLocation;
  private Runnable closeAction = () -> { };
  private boolean dialogDisplayed = true;
  /** True if the file had an alias, which is in the field and meant to stay unless the user changes it. */
  private boolean existingAliasKept;
  private AliasListPanel aliasListPanel;

  /** @param titleKey the message key of the title of the dialog
   * @param descriptionKey the message key of the text which explains the dialog
   * @param linkOf makes the link which the file gets with the location, which is the alias the user typed, the absolute
   *               path of the file if there is no alias, or what a button chose
   * @param relativeLocation what the button for the relative path chooses, null if there is none, which disables the button
   * @param localOffered whether there is a button for the link without a file, which is not for every kind of link
   * @param locationAccepted called with the location, when the user confirms, before the panel closes */
  protected AliasPanel(File file, String titleKey, String descriptionKey, Function<LinkLocation, String> linkOf,
                       LinkLocation relativeLocation, boolean localOffered, Consumer<LinkLocation> locationAccepted) {
    super(new FormLayout("10px, right:default, 8px, fill:default:grow, 10px",
      "pref, 10px, pref, 4px, pref, 4px, pref, 4px, pref, 10px, pref, 10px"));
    this.file = file;
    this.titleKey = titleKey;
    this.linkOf = linkOf;
    this.locationAccepted = locationAccepted;
    aliasField.setName(ALIAS_FIELD);
    ((AbstractDocument) aliasField.getDocument()).setDocumentFilter(new AliasInputFilter());
    previewField.setName(PREVIEW_FIELD);
    previewField.setEditable(false);
    makeLookLikeLabel(previewField);
    problemLabel.setName(PROBLEM_LABEL);
    problemLabel.setForeground(Styles.PROBLEM_COLOR);
    allAliases.setName(ALL_ALIASES_BUTTON);
    makeLookLikeWebLink(allAliases);
    allAliases.addActionListener(e -> showAllAliases());
    ok.setName(OK_BUTTON);
    ok.addActionListener(e -> confirm());
    cancel.setName(CANCEL_BUTTON);
    cancel.addActionListener(e -> closeAction.run());
    add(new IHeaderPanel(titleKey, descriptionKey, ICON), CC.xyw(1, 1, 5));
    add(new ILabel("dialog.link.alias.label"), CC.xy(2, 3));
    add(buildAliasPanel(), CC.xy(4, 3, "left, center"));
    add(new ILabel("dialog.link.alias.path.label"), CC.xy(2, 5));
    add(buildPathPanel(relativeLocation, localOffered), CC.xy(4, 5, "left, center"));
    add(new ILabel("dialog.link.alias.preview.label"), CC.xy(2, 7));
    add(previewField, CC.xy(4, 7));
    add(problemLabel, CC.xy(4, 9));
    add(buildButtonPanel(), CC.xyw(2, 11, 3));
    aliasField.getDocument().addDocumentListener(new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e) { aliasTyped(); }
      @Override public void removeUpdate(DocumentEvent e) { aliasTyped(); }
      @Override public void changedUpdate(DocumentEvent e) { aliasTyped(); }
    });
    prefillAlias();
    registerEscapeAsCancel();
    refresh();
  }

  /** @return the message key of the title of the dialog around the panel */
  String titleKey() {
    return titleKey;
  }

  /** Defines what to do when the panel is done, i.e. after the user confirmed or cancelled. */
  public void onClose(Runnable closeAction) {
    this.closeAction = closeAction;
  }

  /** Allows tests to operate the {@link AliasListPanel} without displaying the dialog around it */
  public void setDialogDisplayed(boolean dialogDisplayed) {
    this.dialogDisplayed = dialogDisplayed;
  }

  /** The panel of the dialog opened by the last click on the link to all aliases, null if there was none. */
  public AliasListPanel aliasListPanel() {
    return aliasListPanel;
  }

  private void showAllAliases() {
    aliasListPanel = new AliasListPanel();
    if (dialogDisplayed) {
      new AliasListDialog(SwingUtilities.getWindowAncestor(this), aliasListPanel);
      // The user may have freed the alias just typed, or changed what the link looks like
      refresh();
    }
  }

  /** Once the panel is part of a window, Enter confirms. The cursor is where the user types the alias, except if the file
   * has one, which the user is likely to keep: then it is on the button to confirm. */
  @Override
  public void addNotify() {
    super.addNotify();
    getRootPane().setDefaultButton(ok);
    (existingAliasKept ? ok : aliasField).requestFocusInWindow();
  }

  private void makeLookLikeLabel(JTextField field) {
    field.setOpaque(false);
    // A plain Color, not the UIResource of the look and feel, which would paint a read-only field gray
    field.setBackground(new Color(getBackground().getRGB()));
    field.setBorder(BorderFactory.createEmptyBorder());
  }

  /** Offers the alias which the file has. It is neither selected nor is the cursor in the field, so a key which the user
   * presses by accident doesn't spoil it: the user most likely wants to keep it, and Enter does so. A file which doesn't
   * have one, and about which the user hasn't decided yet, gets an alias made like the existing ones, selected so the user
   * can take it over or just type another one. A suggestion which isn't valid for the file or already used for another
   * file is dropped. A file the user decided against an alias for gets none. */
  private void prefillAlias() {
    String alias = LinkAliases.aliasOf(file);
    if (!alias.isEmpty()) {
      aliasField.setText(alias);
      existingAliasKept = true;
      return;
    }
    if (!LinkAliases.isAssigned(file)) {
      String suggestion = new AliasSuggester(LinkAliases.entries()).suggest(file);
      if (suggestion != null && LinkAliases.problemKeyFor(file, suggestion) == null) {
        aliasField.setText(suggestion);
        aliasField.selectAll();
      }
    }
  }

  /** Typing, or taking away what was typed, hands the decision back to the alias field. An empty alias field means the
   * absolute path of the file. */
  private void aliasTyped() {
    chosenLocation = null;
    refresh();
  }

  /** What the button chose takes the place of the alias, so the alias field is emptied. */
  private void choose(LinkLocation location) {
    aliasField.setText("");
    chosenLocation = location;
    refresh();
  }

  private LinkLocation currentLocation() {
    return chosenLocation != null ? chosenLocation : LinkLocation.of(file, aliasField.getText());
  }

  private void refresh() {
    String problemKey = chosenLocation != null ? null : LinkAliases.problemKeyFor(file, aliasField.getText());
    ok.setEnabled(problemKey == null);
    problemLabel.setText(problemKey == null ? NO_PROBLEM_TEXT : i18n(problemKey));
    previewField.setText(problemKey == null ? linkOf.apply(currentLocation()) : "");
  }

  private void confirm() {
    locationAccepted.accept(currentLocation());
    closeAction.run();
  }

  private JPanel buildAliasPanel() {
    JPanel panel = new JPanel(new FormLayout("default, 8px, default", "pref"));
    panel.add(aliasField, CC.xy(1, 1));
    panel.add(allAliases, CC.xy(3, 1));
    return panel;
  }

  private JPanel buildPathPanel(LinkLocation relativeLocation, boolean localOffered) {
    JPanel panel = new JPanel(new FormLayout("default, 6px, default, 6px, default", "pref"));
    panel.add(pathButton("absolute", ABSOLUTE_BUTTON, LinkLocation.absolute(file)), CC.xy(1, 1));
    JButton relative = pathButton("relative", RELATIVE_BUTTON, relativeLocation);
    relative.setEnabled(relativeLocation != null);
    panel.add(relative, CC.xy(3, 1));
    if (localOffered) {
      panel.add(pathButton("local", LOCAL_BUTTON, LinkLocation.local()), CC.xy(5, 1));
    }
    return panel;
  }

  private JButton pathButton(String messageKeySuffix, String name, LinkLocation location) {
    JButton button = new IButton("dialog.link.alias." + messageKeySuffix);
    button.setName(name);
    button.setToolTipText(i18n("dialog.link.alias." + messageKeySuffix + ".tooltip"));
    button.addActionListener(e -> choose(location));
    return button;
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
