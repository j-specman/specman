package classman.editor;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/** The vertical tool bar at the left edge of the editor window. Buttons show an icon and a text, or an icon only,
 * which the button at the bottom switches. The most important element comes first, at the top. */
class EditorPalette extends JToolBar {
  private static final String PREF_SHOW_TEXT = "paletteShowText";
  private static final float TEXT_SCALE = 1.2f;
  private static final int ICON_TEXT_GAP = 8;

  private final EditorTexts texts;
  private final List<JButton> elementButtons = new ArrayList<>();
  private final JButton toggleButton = new JButton();
  private final List<String> labels = new ArrayList<>();
  private final List<String> tooltips = new ArrayList<>();
  private boolean showText;

  EditorPalette(EditorTexts texts, Consumer<Point> classDropped) {
    super(JToolBar.VERTICAL);
    this.texts = texts;
    this.showText = Preferences.userNodeForPackage(EditorPalette.class).getBoolean(PREF_SHOW_TEXT, true);
    setFloatable(false);

    toggleButton.setMargin(new Insets(0, 0, 0, 0));
    toggleButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    toggleButton.addActionListener(e -> toggleShowText());

    addDragButton("class", EditorIcons.CLASS, classDropped);

    add(Box.createVerticalGlue());
    addSeparator();
    add(toggleButton);
    applyShowText();
  }

  private void addDragButton(String key, String iconPath, Consumer<Point> dropped) {
    JButton button = new JButton();
    Icon icon = EditorIcons.read(iconPath, EditorIcons.SIZE);
    button.setIcon(icon);
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setFont(button.getFont().deriveFont(button.getFont().getSize2D() * TEXT_SCALE));
    button.setIconTextGap(ICON_TEXT_GAP);
    button.setHorizontalAlignment(SwingConstants.LEADING);
    button.setHorizontalTextPosition(SwingConstants.TRAILING);
    button.setAlignmentX(Component.LEFT_ALIGNMENT);
    String label = texts.get("palette.label." + key);
    PaletteDragAdapter dragAdapter = new PaletteDragAdapter(label, icon, dropped);
    button.addMouseListener(dragAdapter);
    button.addMouseMotionListener(dragAdapter);
    elementButtons.add(button);
    labels.add(label);
    tooltips.add(texts.get("palette.create." + key));
    add(button);
  }

  private void toggleShowText() {
    showText = !showText;
    Preferences.userNodeForPackage(EditorPalette.class).putBoolean(PREF_SHOW_TEXT, showText);
    applyShowText();
    Container topLevel = getTopLevelAncestor();
    if (topLevel != null) {
      topLevel.revalidate();
      topLevel.repaint();
    }
  }

  private void applyShowText() {
    toggleButton.setIcon(EditorIcons.read(showText ? EditorIcons.HIDE_TEXT : EditorIcons.SHOW_TEXT,
      EditorIcons.TOGGLE_SIZE));
    toggleButton.setToolTipText(texts.get(showText ? "palette.toggle.icons.only" : "palette.toggle.icons.text"));
    for (int i = 0; i < elementButtons.size(); i++) {
      JButton button = elementButtons.get(i);
      button.setText(showText ? labels.get(i) : null);
      button.setToolTipText(showText ? null : tooltips.get(i));
      button.setPreferredSize(null);
      button.setMaximumSize(null);
    }
    toggleButton.setMaximumSize(null);
    if (showText) {
      equalizeButtonWidths();
    }
  }

  /** Gives all buttons, including the toggle button, the width of the widest one. */
  private void equalizeButtonWidths() {
    int maxWidth = elementButtons.stream().mapToInt(b -> b.getPreferredSize().width).max().orElse(0);
    for (JButton button : elementButtons) {
      button.setPreferredSize(new Dimension(maxWidth, button.getPreferredSize().height));
      button.setMaximumSize(button.getPreferredSize());
    }
    toggleButton.setMaximumSize(new Dimension(maxWidth, toggleButton.getPreferredSize().height));
  }
}
