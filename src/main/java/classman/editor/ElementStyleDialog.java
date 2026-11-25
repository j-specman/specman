package classman.editor;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.FontFamily_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.StyleProperty_V001;
import classman.render.DiagramFonts;
import classman.render.DiagramRenderer;
import classman.render.Scene;
import classman.render.TextFont;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** The dialog in which the user chooses the look of a class, note or relation: colors, font and font size, as far as
 * the element has them. A preview shows the element in the style, and next to it the user can switch to each change
 * set, to see whether the colors go with the colors of the change marks. The dialog works on a copy of the style.
 * What the preview shows is the business of the subclass for the kind of element. */
abstract class ElementStyleDialog extends JDialog {
  private static final int PREVIEW_FONT_SIZE = 14;

  /** Lays out the preview and knows the colors of the change sets. */
  protected final DiagramRenderer renderer;
  private final EditorTexts texts;
  private final ElementStyleModel_V001 style;
  private final Set<StyleProperty_V001> properties;
  private final StylePreview preview;
  private final ButtonGroup markGroup = new ButtonGroup();
  private final List<JRadioButton> markButtons = new ArrayList<>();
  private final List<ColorChoice> colorChoices = new ArrayList<>();
  private final JComboBox<FontFamily_V001> fontCombo = new JComboBox<>(FontFamily_V001.values());
  private final JComboBox<Integer> sizeCombo = new JComboBox<>(ElementStyleModel_V001.FONT_SIZES.toArray(new Integer[0]));
  private final JButton resetButton;
  private final JButton okButton;
  private ElementStyleModel_V001 result;
  private String shownChangeSet;

  /** @param start the style of the element, which the dialog doesn't change
   * @param changeSets the names of the change sets the preview can show
   * @param titleKey the key of the title of the dialog
   * @param properties the parts of the style which the element has, there is a row for each of them
   * @param standard the colors of the element without colors of its own */
  protected ElementStyleDialog(Window owner, ElementStyleModel_V001 start, DiagramRenderer renderer,
                               List<String> changeSets, EditorTexts texts, String titleKey,
                               Set<StyleProperty_V001> properties, StandardColors standard) {
    super(owner, texts.get(titleKey), ModalityType.APPLICATION_MODAL);
    this.renderer = renderer;
    this.texts = texts;
    this.style = start.copy();
    this.properties = properties;
    this.preview = new StylePreview(this::previewScene);
    this.okButton = new JButton(UIManager.getString("OptionPane.okButtonText"));
    this.resetButton = new JButton(texts.get("style.dialog.standard.all"));
    prepareFontCombo();
    prepareSizeCombo();
    setLayout(new FormLayout("pref, 8px, fill:pref:grow", "fill:pref, 8px, pref"));
    ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    add(form(standard), CC.xy(1, 1));
    add(previewPanel(changeSets), CC.xy(3, 1));
    add(buttons(), CC.xyw(1, 3, 3));
    refreshPreview();
    setResizable(false);
    pack();
    setLocationRelativeTo(owner);
  }

  /** @return the dialog for the kind of element */
  static ElementStyleDialog forElement(Component parent, AbstractDiagramElementModel_V001 element,
                                       ElementStyleModel_V001 start, DiagramRenderer renderer,
                                       List<String> changeSets, EditorTexts texts) {
    Window owner = SwingUtilities.getWindowAncestor(parent);
    if (element instanceof NoteModel_V001) {
      return new NoteStyleDialog(owner, start, renderer, changeSets, texts);
    }
    if (element instanceof AbstractRelationModel_V001) {
      return new RelationStyleDialog(owner, start, renderer, changeSets, texts);
    }
    String stereotype = element instanceof ClassModel_V001 box ? box.stereotype : null;
    return new ClassStyleDialog(owner, start, renderer, changeSets, texts, stereotype);
  }

  /** Shows the dialog and waits for the user.
   * @return the style the user has chosen, null if the user cancels */
  ElementStyleModel_V001 choose() {
    setVisible(true);
    return result;
  }

  /** @return the picture of the element in the style, with the marks of the change set if there is one. It is called
   * when the preview is painted, never while the dialog is built. */
  protected abstract Scene previewScene(ElementStyleModel_V001 previewStyle, String changeSet);

  /** @return the style the user has confirmed, null while the user hasn't */
  ElementStyleModel_V001 result() { return result; }

  StylePreview preview() { return preview; }

  JComboBox<FontFamily_V001> fontCombo() { return fontCombo; }

  JComboBox<Integer> sizeCombo() { return sizeCombo; }

  JButton okButton() { return okButton; }

  JButton resetButton() { return resetButton; }

  /** @return the choices of the colors the element has, in the order fill, text, line */
  List<ColorChoice> colorChoices() { return colorChoices; }

  /** @return the radio buttons: first the one for the unmarked element, then one for each change set */
  List<JRadioButton> markButtons() { return markButtons; }

  private JPanel form(StandardColors standard) {
    Map<String, JComponent> rows = new LinkedHashMap<>();
    if (properties.contains(StyleProperty_V001.FILL)) {
      rows.put("style.dialog.fill", colorChoice("style.dialog.fill", standard.fill, style.fill,
        color -> style.fill = color));
    }
    if (properties.contains(StyleProperty_V001.TEXT_COLOR)) {
      rows.put("style.dialog.text", colorChoice("style.dialog.text", standard.text, style.textColor,
        color -> style.textColor = color));
    }
    if (properties.contains(StyleProperty_V001.LINE_COLOR)) {
      rows.put("style.dialog.line", colorChoice("style.dialog.line", standard.line, style.lineColor,
        color -> style.lineColor = color));
    }
    if (properties.contains(StyleProperty_V001.FONT)) {
      rows.put("style.dialog.font", fontCombo);
    }
    if (properties.contains(StyleProperty_V001.FONT_SIZE)) {
      rows.put("style.dialog.size", sizeCombo);
    }
    return formOf(rows);
  }

  private JPanel formOf(Map<String, JComponent> rows) {
    StringBuilder rowSpecs = new StringBuilder();
    for (int i = 0; i < rows.size(); i++) {
      rowSpecs.append(i == 0 ? "pref" : ", 8px, pref");
    }
    JPanel form = new JPanel(new FormLayout("right:pref, 12px, fill:pref", rowSpecs.toString()));
    int row = 1;
    for (Map.Entry<String, JComponent> entry : rows.entrySet()) {
      form.add(new JLabel(texts.get(entry.getKey())), CC.xy(1, row));
      form.add(entry.getValue(), CC.xy(3, row));
      row += 2;
    }
    return form;
  }

  private ColorChoice colorChoice(String titleKey, String standardColor, String start, Consumer<String> setter) {
    ColorChoice choice = new ColorChoice(texts.get(titleKey), standardColor, start, texts, color -> {
      setter.accept(color);
      refreshPreview();
    });
    colorChoices.add(choice);
    return choice;
  }

  /** The names of the fonts are shown in their own fonts. */
  private void prepareFontCombo() {
    DefaultListCellRenderer base = new DefaultListCellRenderer();
    fontCombo.setRenderer((list, value, index, selected, focus) -> {
      JLabel label = (JLabel) base.getListCellRendererComponent(list, value, index, selected, focus);
      label.setText(value.sourceName);
      label.setFont(DiagramFonts.plain(value, PREVIEW_FONT_SIZE));
      return label;
    });
    fontCombo.setSelectedItem(style.font != null ? style.font : TextFont.DEFAULT.family);
    fontCombo.addActionListener(e -> {
      FontFamily_V001 chosen = (FontFamily_V001) fontCombo.getSelectedItem();
      style.font = chosen == TextFont.DEFAULT.family ? null : chosen;
      refreshPreview();
    });
  }

  private void prepareSizeCombo() {
    sizeCombo.setSelectedItem(style.fontSize != null ? style.fontSize : TextFont.DEFAULT.size);
    sizeCombo.addActionListener(e -> {
      Integer chosen = (Integer) sizeCombo.getSelectedItem();
      style.fontSize = chosen == TextFont.DEFAULT.size ? null : chosen;
      refreshPreview();
    });
  }

  private JPanel previewPanel(List<String> changeSets) {
    JPanel panel = new JPanel(new FormLayout("fill:pref:grow, 8px, fill:pref", "fill:pref:grow"));
    panel.setBorder(BorderFactory.createTitledBorder(texts.get("style.dialog.preview")));
    panel.add(preview, CC.xy(1, 1));
    panel.add(markRows(changeSets), CC.xy(3, 1));
    return panel;
  }

  /** One row for the unmarked element and one for each change set, all of the same height. */
  private JPanel markRows(List<String> changeSets) {
    StringBuilder rowSpecs = new StringBuilder("fill:pref:grow");
    for (int i = 0; i < changeSets.size(); i++) {
      rowSpecs.append(", 2px, fill:pref:grow");
    }
    JPanel marks = new JPanel(new FormLayout("fill:pref", rowSpecs.toString()));
    marks.add(markRow(texts.get("style.dialog.unmarked"), null), CC.xy(1, 1));
    for (int i = 0; i < changeSets.size(); i++) {
      String changeSet = changeSets.get(i);
      marks.add(markRow(changeSet, changeSet), CC.xy(1, 3 + 2 * i));
    }
    return marks;
  }

  /** A radio button which switches the preview to the change set. Like the items of Specman's change set menu, the row
   * has the pale color of the change set and the name has the strong one behind it. */
  private JPanel markRow(String label, String changeSet) {
    JRadioButton button = new JRadioButton(label, changeSet == null);
    button.addActionListener(e -> {
      shownChangeSet = changeSet;
      refreshPreview();
    });
    markGroup.add(button);
    markButtons.add(button);
    JPanel row = new JPanel(new FormLayout("2px, left:pref", "center:pref:grow"));
    row.add(button, CC.xy(2, 1));
    if (changeSet != null) {
      button.setText("<html><table cellspacing='0' cellpadding='1'><tr><td bgcolor='" + renderer.strongColorOf(changeSet)
        + "'>" + label + "</td></tr></table></html>");
      button.setOpaque(false);
      row.setBackground(Color.decode(renderer.paleColorOf(changeSet)));
    }
    return row;
  }

  private JPanel buttons() {
    JPanel buttons = new JPanel(new FormLayout("pref, fill:pref:grow, pref, 6px, pref", "pref"));
    resetButton.addActionListener(e -> resetToStandard());
    okButton.addActionListener(e -> {
      result = style.copy();
      dispose();
    });
    JButton cancel = new JButton(UIManager.getString("OptionPane.cancelButtonText"));
    cancel.addActionListener(e -> dispose());
    buttons.add(resetButton, CC.xy(1, 1));
    buttons.add(okButton, CC.xy(3, 1));
    buttons.add(cancel, CC.xy(5, 1));
    getRootPane().setDefaultButton(okButton);
    return buttons;
  }

  /** Takes all parts of the style back to the standard: the colors, the font and its size. */
  private void resetToStandard() {
    style.fill = null;
    style.textColor = null;
    style.lineColor = null;
    style.font = null;
    style.fontSize = null;
    colorChoices.forEach(ColorChoice::showStandard);
    fontCombo.setSelectedItem(TextFont.DEFAULT.family);
    sizeCombo.setSelectedItem(TextFont.DEFAULT.size);
    refreshPreview();
  }

  /** The button which resets everything can only be pressed while something is not standard. */
  private void refreshPreview() {
    preview.show(style, shownChangeSet);
    resetButton.setEnabled(!style.isStandard());
  }
}
