package classman.editor;

import classman.render.SceneColor;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import java.awt.Color;
import java.util.function.Consumer;

/** One color of the style dialog: a button which shows the color and opens the color chooser, and a button which takes
 * the color back to the standard, which can't be pressed while the color is the standard. The first button shows the
 * standard color then. Choosing the standard color is the same as pressing the second button. */
class ColorChoice extends JPanel {
  private final String title;
  private final String standardColor;
  private final JButton choose = new JButton();
  private final JButton standard;
  private String color;

  /** @param standardColor the color which the box has without one of its own, as #rrggbb or #rrggbbaa
   * @param start the color of the style, null for the standard one
   * @param changed gets the color the user has chosen as #rrggbb, or #rrggbbaa if the user made it transparent, or
   * null for the standard one */
  ColorChoice(String title, String standardColor, String start, EditorTexts texts, Consumer<String> changed) {
    super(new FormLayout("pref, 4px, pref", "pref"));
    this.title = title;
    this.standardColor = standardColor;
    color = start;
    standard = new JButton(texts.get("style.dialog.standard"));
    choose.addActionListener(e -> {
      Color chosen = JColorChooser.showDialog(this, title, SceneColor.parse(shownColor()));
      if (chosen != null) {
        String hex = SceneColor.of(chosen);
        color = hex.equalsIgnoreCase(standardColor) ? null : hex;
        update(changed);
      }
    });
    standard.addActionListener(e -> {
      color = null;
      update(changed);
    });
    add(choose, CC.xy(1, 1));
    add(standard, CC.xy(3, 1));
    showColor();
  }

  /** @return the color the button shows: the chosen one, or the standard one */
  String shownColor() {
    return color != null ? color : standardColor;
  }

  /** @return the button which opens the color chooser */
  JButton chooseButton() {
    return choose;
  }

  /** Shows the standard color, without telling anyone: for the dialog which takes the whole style back to standard. */
  void showStandard() {
    color = null;
    showColor();
  }

  private void update(Consumer<String> changed) {
    showColor();
    changed.accept(color);
  }

  private void showColor() {
    choose.setIcon(new ColorSwatchIcon(shownColor()));
    standard.setEnabled(color != null);
    choose.setToolTipText(title);
  }
}
