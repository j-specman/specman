package classman.editor;

import classman.model.v001.ClassStyleModel_V001;
import classman.model.v001.FontFamily_V001;
import classman.render.DiagramFonts;
import classman.render.DiagramRenderer;
import classman.render.DiagramStyle;
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
import java.util.List;
import java.util.function.Consumer;

/** The dialog in which the user chooses the look of a class: colors, font and font size. A preview shows a class in
 * the style, and next to it the user can switch to each change set, to see whether the colors go with the colors of
 * the change marks. The dialog works on a copy of the style. */
class ClassStyleDialog extends JDialog {
  private static final int PREVIEW_FONT_SIZE = 14;

  private final EditorTexts texts;
  private final ClassStyleModel_V001 style;
  private final StylePreview preview;
  private final ButtonGroup markGroup = new ButtonGroup();
  private final List<JRadioButton> markButtons = new ArrayList<>();
  private final List<ColorChoice> colorChoices = new ArrayList<>();
  private final JComboBox<FontFamily_V001> fontCombo = new JComboBox<>(FontFamily_V001.values());
  private final JComboBox<Integer> sizeCombo = new JComboBox<>(ClassStyleModel_V001.FONT_SIZES.toArray(new Integer[0]));
  private final JButton okButton;
  private ClassStyleModel_V001 result;
  private String shownChangeSet;

  /** @param start the style of the class, which the dialog doesn't change
   * @param changeSets the names of the change sets the preview can show */
  ClassStyleDialog(Window owner, ClassStyleModel_V001 start, DiagramRenderer renderer,
                   List<String> changeSets, EditorTexts texts) {
    super(owner, texts.get("style.dialog.title"), ModalityType.APPLICATION_MODAL);
    this.texts = texts;
    this.style = start.copy();
    this.preview = new StylePreview(renderer);
    this.okButton = new JButton(UIManager.getString("OptionPane.okButtonText"));
    prepareFontCombo();
    prepareSizeCombo();
    setLayout(new FormLayout("pref, 8px, fill:pref:grow", "fill:pref, 8px, pref"));
    ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    add(form(), CC.xy(1, 1));
    add(previewPanel(renderer, changeSets), CC.xy(3, 1));
    add(buttons(), CC.xyw(1, 3, 3));
    refreshPreview();
    setResizable(false);
    pack();
    setLocationRelativeTo(owner);
  }

  /** Shows the dialog and waits for the user.
   * @return the style the user has chosen, null if the user cancels */
  static ClassStyleModel_V001 choose(Component parent, ClassStyleModel_V001 start,
                                     DiagramRenderer renderer, List<String> changeSets, EditorTexts texts) {
    ClassStyleDialog dialog = new ClassStyleDialog(SwingUtilities.getWindowAncestor(parent), start,
      renderer, changeSets, texts);
    dialog.setVisible(true);
    return dialog.result;
  }

  /** @return the style the user has confirmed, null while the user hasn't */
  ClassStyleModel_V001 result() { return result; }

  StylePreview preview() { return preview; }

  JComboBox<FontFamily_V001> fontCombo() { return fontCombo; }

  JComboBox<Integer> sizeCombo() { return sizeCombo; }

  JButton okButton() { return okButton; }

  /** @return the choices of the fill, text and line color, in this order */
  List<ColorChoice> colorChoices() { return colorChoices; }

  /** @return the radio buttons: first the one for the unmarked class, then one for each change set */
  List<JRadioButton> markButtons() { return markButtons; }

  private JPanel form() {
    JPanel form = new JPanel(new FormLayout("right:pref, 12px, fill:pref",
      "pref, 8px, pref, 8px, pref, 8px, pref, 8px, pref"));
    addRow(form, 1, "style.dialog.fill", colorChoice("style.dialog.fill", DiagramStyle.CLASS_FILL, style.fill,
      color -> style.fill = color));
    addRow(form, 3, "style.dialog.text", colorChoice("style.dialog.text", DiagramStyle.TEXT_COLOR, style.textColor,
      color -> style.textColor = color));
    addRow(form, 5, "style.dialog.line", colorChoice("style.dialog.line", DiagramStyle.STROKE, style.lineColor,
      color -> style.lineColor = color));
    addRow(form, 7, "style.dialog.font", fontCombo);
    addRow(form, 9, "style.dialog.size", sizeCombo);
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

  private void addRow(JPanel form, int row, String labelKey, JComponent control) {
    form.add(new JLabel(texts.get(labelKey)), CC.xy(1, row));
    form.add(control, CC.xy(3, row));
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

  private JPanel previewPanel(DiagramRenderer renderer, List<String> changeSets) {
    JPanel panel = new JPanel(new FormLayout("fill:pref:grow, 8px, fill:pref", "fill:pref:grow"));
    panel.setBorder(BorderFactory.createTitledBorder(texts.get("style.dialog.preview")));
    panel.add(preview, CC.xy(1, 1));
    panel.add(markRows(renderer, changeSets), CC.xy(3, 1));
    return panel;
  }

  /** One row for the unmarked class and one for each change set, all of the same height. */
  private JPanel markRows(DiagramRenderer renderer, List<String> changeSets) {
    StringBuilder rowSpecs = new StringBuilder("fill:pref:grow");
    for (int i = 0; i < changeSets.size(); i++) {
      rowSpecs.append(", 2px, fill:pref:grow");
    }
    JPanel marks = new JPanel(new FormLayout("fill:pref", rowSpecs.toString()));
    marks.add(markRow(texts.get("style.dialog.unmarked"), null, renderer), CC.xy(1, 1));
    for (int i = 0; i < changeSets.size(); i++) {
      String changeSet = changeSets.get(i);
      marks.add(markRow(changeSet, changeSet, renderer), CC.xy(1, 3 + 2 * i));
    }
    return marks;
  }

  /** A radio button which switches the preview to the change set. Like the items of Specman's change set menu, the row
   * has the pale color of the change set and the name has the strong one behind it. */
  private JPanel markRow(String label, String changeSet, DiagramRenderer renderer) {
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
    JPanel buttons = new JPanel(new FormLayout("fill:pref:grow, pref, 6px, pref", "pref"));
    okButton.addActionListener(e -> {
      result = style.copy();
      dispose();
    });
    JButton cancel = new JButton(UIManager.getString("OptionPane.cancelButtonText"));
    cancel.addActionListener(e -> dispose());
    buttons.add(okButton, CC.xy(2, 1));
    buttons.add(cancel, CC.xy(4, 1));
    getRootPane().setDefaultButton(okButton);
    return buttons;
  }

  private void refreshPreview() {
    preview.show(style, shownChangeSet);
  }
}
