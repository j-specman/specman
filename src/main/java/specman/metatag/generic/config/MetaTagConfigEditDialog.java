package specman.metatag.generic.config;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.graphics.SvgIcon;
import specman.metatag.MetaTagPanel;
import specman.metatag.generic.GenericMetaTag;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static specman.view.AbstractSchrittView.LINIENBREITE;

/** Modal form for creating or editing one {@link MetaTagConfig}. The result is only delivered if
 * the user confirms with OK; nothing is changed in the registry by this dialog itself. */
class MetaTagConfigEditDialog extends JDialog {
  private static final Color DEFAULT_BACKGROUND = new Color(0xE0E0E0);
  private static final Color DEFAULT_BORDER_COLOR = new Color(0x606060);
  private static final int ICON_PREVIEW_SIZE = 16;
  private static final int TEXT_FIELD_COLUMNS = 20;
  private static final int PREVIEW_INDENT = 8;
  private static final int PREVIEW_BOTTOM_PADDING = 8;

  private final Predicate<String> isNameTaken;
  private final JTextField nameField = new JTextField(TEXT_FIELD_COLUMNS);
  private final JTextField labelField = new JTextField(TEXT_FIELD_COLUMNS);
  private final JLabel iconPreview = new JLabel("no icon chosen");
  private final JButton backgroundButton = new JButton();
  private final JButton borderButton = new JButton();
  private final JPanel tagHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, PREVIEW_INDENT, 0));
  private final JPanel tagPreview = new JPanel(new BorderLayout());

  private String iconSvg;
  private Color backgroundColor = DEFAULT_BACKGROUND;
  private Color borderColor = DEFAULT_BORDER_COLOR;
  private boolean labelFollowsName = true;
  private MetaTagConfig result;

  /** @param existing the configuration to edit, null to create a new one
   * @param isNameTaken tells whether a name is already used by another configuration */
  MetaTagConfigEditDialog(Frame owner, MetaTagConfig existing, Predicate<String> isNameTaken) {
    super(owner, existing == null ? "New Meta Tag" : "Edit Meta Tag", true);
    this.isNameTaken = isNameTaken;
    if (existing != null) {
      nameField.setText(existing.getName());
      labelField.setText(existing.getLabel());
      iconSvg = existing.getIconSvg();
      backgroundColor = existing.getBackgroundColor();
      borderColor = existing.getBorderColor();
    }
    labelFollowsName = labelMatchesName();
    refreshIconAndColors();
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
    ((AbstractDocument) nameField.getDocument()).setDocumentFilter(new IdentifierFilter());
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

  private JPanel buildFormPanel() {
    JPanel panel = new JPanel(new FormLayout("10px, right:default, 8px, fill:default:grow, 10px",
      "10px, pref, 4px, pref, 4px, pref, 4px, pref, 4px, pref, 4px, pref, 10px"));
    panel.add(new JLabel("Name:"), CC.xy(2, 2));
    panel.add(nameField, CC.xy(4, 2));
    panel.add(new JLabel("Label:"), CC.xy(2, 4));
    panel.add(labelField, CC.xy(4, 4));
    panel.add(new JLabel("Icon:"), CC.xy(2, 6));
    panel.add(buildIconPanel(), CC.xy(4, 6));
    panel.add(new JLabel("Background:"), CC.xy(2, 8));
    panel.add(backgroundButton, CC.xy(4, 8, "left, center"));
    panel.add(new JLabel("Border:"), CC.xy(2, 10));
    panel.add(borderButton, CC.xy(4, 10, "left, center"));
    panel.add(new JLabel("Preview:"), CC.xy(2, 12, "right, top"));
    panel.add(buildPreviewPanel(), CC.xy(4, 12));
    backgroundButton.addActionListener(e -> chooseBackground());
    borderButton.addActionListener(e -> chooseBorderColor());
    return panel;
  }

  /** The tag hangs below a black line, like below the top edge of a step. */
  private JPanel buildPreviewPanel() {
    JPanel stepEdge = new JPanel();
    stepEdge.setBackground(Color.BLACK);
    stepEdge.setPreferredSize(new Dimension(1, LINIENBREITE));
    tagHolder.setBackground(Color.WHITE);
    tagHolder.setPreferredSize(new Dimension(0, previewHeight()));
    tagPreview.add(stepEdge, BorderLayout.NORTH);
    tagPreview.add(tagHolder, BorderLayout.CENTER);
    return tagPreview;
  }

  private JPanel buildIconPanel() {
    JButton choose = new JButton("Choose SVG...");
    choose.addActionListener(e -> chooseIcon());
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    panel.add(choose);
    panel.add(new JLabel("  "));
    panel.add(iconPreview);
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

  private void chooseIcon() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new FileNameExtensionFilter("SVG files", "svg"));
    if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File file = chooser.getSelectedFile();
    try {
      String svg = Files.readString(file.toPath(), StandardCharsets.UTF_8);
      new SvgIcon(ICON_PREVIEW_SIZE, svg);
      iconSvg = svg;
      refreshIconAndColors();
      refreshTagPreview();
    }
    catch (IOException | RuntimeException ex) {
      showProblem("The file '" + file.getName() + "' can't be used as icon: " + ex.getMessage());
    }
  }

  private void chooseBackground() {
    Color chosen = JColorChooser.showDialog(this, "Background color", backgroundColor);
    if (chosen != null) {
      backgroundColor = chosen;
      refreshIconAndColors();
      refreshTagPreview();
    }
  }

  private void chooseBorderColor() {
    Color chosen = JColorChooser.showDialog(this, "Border color", borderColor);
    if (chosen != null) {
      borderColor = chosen;
      refreshIconAndColors();
      refreshTagPreview();
    }
  }

  private void refreshIconAndColors() {
    if (iconSvg != null) {
      iconPreview.setText(null);
      iconPreview.setIcon(new SvgIcon(ICON_PREVIEW_SIZE, iconSvg));
    }
    backgroundButton.setIcon(new ColorSwatchIcon(backgroundColor));
    backgroundButton.setText("Choose...");
    borderButton.setIcon(new ColorSwatchIcon(borderColor));
    borderButton.setText("Choose...");
  }

  /** The height a tag has in a step at the current zoom plus some air, reserved from the start so
   * the dialog keeps its size no matter whether an icon was chosen yet. */
  private int previewHeight() {
    return MetaTagPanel.stepnumberHeight() + PREVIEW_BOTTOM_PADDING;
  }

  private void refreshTagPreview() {
    tagHolder.removeAll();
    if (iconSvg != null) {
      GenericMetaTag tag = new GenericMetaTag(currentConfig(), null);
      tag.setFreetextEditable(false);
      tag.setPreferredSize(new Dimension(tag.getPreferredSize().width, MetaTagPanel.stepnumberHeight()));
      tagHolder.add(tag);
    }
    tagHolder.revalidate();
    tagHolder.repaint();
  }

  private MetaTagConfig currentConfig() {
    return new MetaTagConfig(nameField.getText(), labelField.getText(), iconSvg, backgroundColor, borderColor);
  }

  private void confirm() {
    String name = nameField.getText();
    if (name.isEmpty()) {
      showProblem("Please enter a name.");
    }
    else if (isNameTaken.test(name)) {
      showProblem("There is already a meta tag named '" + name + "'.");
    }
    else if (iconSvg == null) {
      showProblem("Please choose an SVG file as icon.");
    }
    else {
      result = currentConfig();
      dispose();
    }
  }

  private void showProblem(String message) {
    JOptionPane.showMessageDialog(this, message, getTitle(), JOptionPane.WARNING_MESSAGE);
  }

  /** Lets only identifier characters (ASCII letters, digits, underscore) into the name field. */
  private static class IdentifierFilter extends DocumentFilter {
    private static final Pattern IDENTIFIER_CHARACTERS = Pattern.compile("[A-Za-z0-9_]*");

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
      if (isAcceptable(string)) {
        super.insertString(fb, offset, string, attr);
      }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
      if (isAcceptable(text)) {
        super.replace(fb, offset, length, text, attrs);
      }
    }

    private boolean isAcceptable(String text) {
      if (text == null || IDENTIFIER_CHARACTERS.matcher(text).matches()) {
        return true;
      }
      Toolkit.getDefaultToolkit().beep();
      return false;
    }
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

  private static class ColorSwatchIcon implements Icon {
    private static final int SIZE = 14;
    private final Color color;

    ColorSwatchIcon(Color color) {
      this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      g.setColor(color);
      g.fillRect(x, y, SIZE, SIZE);
      g.setColor(Color.GRAY);
      g.drawRect(x, y, SIZE - 1, SIZE - 1);
    }

    @Override
    public int getIconWidth() {
      return SIZE;
    }

    @Override
    public int getIconHeight() {
      return SIZE;
    }
  }
}
