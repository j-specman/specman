package specman.metatag.generic.config;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.function.Predicate;

/** Modal form for creating or editing one {@link MetaTagConfig}. The result is only delivered if
 * the user confirms with OK; nothing is changed in the registry by this dialog itself. */
class MetaTagConfigEditDialog extends JDialog {
  private static final Color DEFAULT_BACKGROUND = new Color(0xE0E0E0);
  private static final int TEXT_FIELD_COLUMNS = 20;

  private final Predicate<String> isNameTaken;
  private final MetaTagConfigNameField nameField = new MetaTagConfigNameField(TEXT_FIELD_COLUMNS);
  private final JTextField labelField = new JTextField(TEXT_FIELD_COLUMNS);
  private final IconChooser iconChooser;
  private final ColorChooseButton backgroundButton;
  private final OptionalColorChooser borderChooser;
  private final JCheckBox freetextCheckBox = new JCheckBox("Users can attach a freetext to the tag", true);
  private final PreviewPanel preview = new PreviewPanel();

  private boolean labelFollowsName = true;
  private MetaTagConfig result;

  /** @param existing the configuration to edit, null to create a new one
   * @param nameEditable whether the name may be changed - not if tags at steps refer to it
   * @param isNameTaken tells whether a name is already used by another configuration */
  MetaTagConfigEditDialog(Frame owner, MetaTagConfig existing, boolean nameEditable, Predicate<String> isNameTaken) {
    super(owner, existing == null ? "New Meta Tag" : "Edit Meta Tag", true);
    this.isNameTaken = isNameTaken;
    if (!nameEditable) {
      nameField.setEnabled(false);
      nameField.setToolTipText("Tags at steps refer to this meta tag, so it can't be renamed");
    }
    iconChooser = new IconChooser(existing != null ? existing.getIconSvg() : null, this::refreshTagPreview);
    backgroundButton = new ColorChooseButton("Background color",
      existing != null ? existing.getBackgroundColor() : DEFAULT_BACKGROUND, this::refreshTagPreview);
    borderChooser = new OptionalColorChooser("Border color",
      existing != null ? existing.getBorderColor() : null, this::refreshTagPreview);
    if (existing != null) {
      nameField.setText(existing.getName());
      labelField.setText(existing.getLabel());
      freetextCheckBox.setSelected(existing.allowsFreetext());
    }
    labelFollowsName = labelMatchesName();
    refreshTagPreview();
    initComponents();
  }

  /** @return the confirmed configuration, or null if the user cancelled */
  MetaTagConfig showAndGet() {
    setVisible(true);
    return result;
  }

  private void initComponents() {
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(buildFormPanel(), BorderLayout.CENTER);
    JPanel south = new JPanel(new BorderLayout());
    south.add(new JSeparator(), BorderLayout.NORTH);
    south.add(buildButtonPanel(), BorderLayout.CENTER);
    getContentPane().add(south, BorderLayout.SOUTH);
    nameField.getDocument().addDocumentListener(new SimpleDocumentListener(this::nameChanged));
    labelField.getDocument().addDocumentListener(new SimpleDocumentListener(this::labelChanged));
    pack();
    setResizable(false);
    setLocationRelativeTo(getOwner());
  }

  private boolean labelMatchesName() {
    return labelField.getText().isEmpty() || labelField.getText().equals(nameField.getText());
  }

  /** As long as the label is empty or equal to the name - the typical case - it follows every
   * keystroke in the name field. */
  private void nameChanged() {
    if (labelFollowsName) {
      labelField.setText(nameField.getText());
    }
  }

  private void labelChanged() {
    labelFollowsName = labelMatchesName();
    refreshTagPreview();
  }

  /** The warning about an oversized icon has a row of its own below the icon chooser, which has no
   * height as long as the warning is invisible. */
  private JPanel buildFormPanel() {
    JPanel panel = new JPanel(new FormLayout("10px, right:default, 8px, fill:default:grow, 10px",
      "10px, pref, 4px, pref, 4px, pref, pref, 4px, pref, 4px, pref, 4px, pref, 4px, pref, 10px"));
    panel.add(new JLabel("Name:"), CC.xy(2, 2));
    panel.add(nameField, CC.xy(4, 2));
    panel.add(new JLabel("Label:"), CC.xy(2, 4));
    panel.add(labelField, CC.xy(4, 4));
    panel.add(new JLabel("Icon:"), CC.xy(2, 6));
    panel.add(iconChooser, CC.xy(4, 6));
    panel.add(iconChooser.getSizeWarning(), CC.xy(4, 7));
    panel.add(new JLabel("Background:"), CC.xy(2, 9));
    panel.add(backgroundButton, CC.xy(4, 9, "left, center"));
    panel.add(new JLabel("Border:"), CC.xy(2, 11));
    panel.add(borderChooser, CC.xy(4, 11, "left, center"));
    panel.add(new JLabel("Freetext:"), CC.xy(2, 13));
    panel.add(freetextCheckBox, CC.xy(4, 13));
    panel.add(new JLabel("Preview:"), CC.xy(2, 15, "right, top"));
    panel.add(preview, CC.xy(4, 15));
    return panel;
  }

  private JPanel buildButtonPanel() {
    JButton ok = new JButton("OK");
    ok.addActionListener(e -> confirm());
    JButton cancel = new JButton("Cancel");
    cancel.addActionListener(e -> dispose());
    getRootPane().setDefaultButton(ok);
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.add(ok);
    panel.add(cancel);
    return panel;
  }

  private void refreshTagPreview() {
    MetaTagConfig config = currentConfig();
    preview.showTag(config.hasIcon() || !config.getLabel().isEmpty() ? config : null);
  }

  private MetaTagConfig currentConfig() {
    return new MetaTagConfig(nameField.getText(), labelField.getText(), iconChooser.getIconSvg(),
      backgroundButton.getColor(), borderChooser.getColor(), freetextCheckBox.isSelected());
  }

  private void confirm() {
    String name = nameField.getText();
    if (name.isEmpty()) {
      showProblem("Please enter a name.");
    }
    else if (isNameTaken.test(name)) {
      showProblem("There is already a meta tag named '" + name + "'.");
    }
    else if (iconChooser.getIconSvg() == null && labelField.getText().isEmpty()) {
      showProblem("Please enter a label or choose an SVG file as icon.");
    }
    else {
      result = currentConfig();
      dispose();
    }
  }

  private void showProblem(String message) {
    JOptionPane.showMessageDialog(this, message, getTitle(), JOptionPane.WARNING_MESSAGE);
  }

  private static class SimpleDocumentListener implements DocumentListener {
    private final Runnable onChange;

    SimpleDocumentListener(Runnable onChange) {
      this.onChange = onChange;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
      onChange.run();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      onChange.run();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      onChange.run();
    }
  }
}
