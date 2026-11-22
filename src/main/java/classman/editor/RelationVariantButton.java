package classman.editor;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import classman.model.v001.RelationType_V001;

/** A tool button of the palette which can draw several kinds of relations. It shows the icon and the text of the
 * current kind and has a small arrow at its corner, which opens a menu with all kinds. Choosing one makes it the
 * current kind, which is remembered, and arms the tool for it. */
class RelationVariantButton extends JToggleButton {
  private static final int ARROW_WIDTH = 7;
  private static final int ARROW_HEIGHT = 4;
  private static final int ARROW_ROOM = 12;
  private static final int ARROW_MARGIN = 4;

  private final EditorTexts texts;
  private final List<RelationVariant> variants;
  private final String preferenceKey;
  private final Consumer<RelationType_V001> chosen;
  private RelationVariant current;
  private boolean withText = true;

  /** @param variants the kinds of the button, the first one is current as long as the user hasn't chosen
   * @param preferenceKey where the choice is remembered
   * @param chosen is run with the type of a kind which the user chose in the menu */
  RelationVariantButton(EditorTexts texts, List<RelationVariant> variants, String preferenceKey,
                        Consumer<RelationType_V001> chosen) {
    this.texts = texts;
    this.variants = variants;
    this.preferenceKey = preferenceKey;
    this.chosen = chosen;
    this.current = remembered();
    show(current);
  }

  RelationType_V001 currentType() {
    return current.type;
  }

  String currentLabel() {
    return labelOf(current);
  }

  /** @return true if the button draws relations of the type */
  boolean offers(RelationType_V001 type) {
    return variants.stream().anyMatch(v -> v.type == type);
  }

  /** Shows the kind with the type as the current one, without remembering it. */
  void showType(RelationType_V001 type) {
    for (RelationVariant variant : variants) {
      if (variant.type == type) {
        show(variant);
      }
    }
  }

  private RelationVariant remembered() {
    String name = Preferences.userNodeForPackage(RelationVariantButton.class).get(preferenceKey, null);
    for (RelationVariant variant : variants) {
      if (variant.type.name().equals(name)) {
        return variant;
      }
    }
    return variants.get(0);
  }

  private void show(RelationVariant variant) {
    current = variant;
    setIcon(EditorIcons.read(variant.iconPath, EditorIcons.SIZE));
    refreshTexts();
  }

  private String labelOf(RelationVariant variant) {
    return texts.get("palette.label." + variant.key());
  }

  /** Shows the text of the current kind, or none. */
  void showText(boolean withText) {
    this.withText = withText;
    refreshTexts();
  }

  private void refreshTexts() {
    setText(withText ? labelOf(current) : null);
    setToolTipText(withText ? null : texts.get("palette.create." + current.key()));
  }

  /** The button is as wide as it needs to be for the longest text of its kinds, so it doesn't change its width when
   * the user chooses another one. */
  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    if (getText() == null || isPreferredSizeSet()) {
      return size;
    }
    FontMetrics metrics = getFontMetrics(getFont());
    int widest = variants.stream().mapToInt(v -> metrics.stringWidth(labelOf(v))).max().orElse(0);
    return new Dimension(size.width - metrics.stringWidth(getText()) + widest, size.height);
  }

  @Override
  public Insets getInsets() {
    Insets insets = super.getInsets();
    return new Insets(insets.top, insets.left, insets.bottom, insets.right + ARROW_ROOM);
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    boolean isPress = e.getID() == MouseEvent.MOUSE_PRESSED || e.getID() == MouseEvent.MOUSE_RELEASED
      || e.getID() == MouseEvent.MOUSE_CLICKED;
    if (isPress && isInArrowZone(e.getPoint()) && !getModel().isPressed()) {
      if (e.getID() == MouseEvent.MOUSE_PRESSED && isEnabled()) {
        openMenu();
      }
      e.consume();
      return;
    }
    super.processMouseEvent(e);
  }

  private boolean isInArrowZone(Point at) {
    return at.x >= getWidth() - ARROW_ROOM - ARROW_MARGIN;
  }

  private void openMenu() {
    JPopupMenu menu = new JPopupMenu();
    for (RelationVariant variant : variants) {
      Icon icon = EditorIcons.read(variant.iconPath, EditorIcons.TOGGLE_SIZE);
      JMenuItem item = new JMenuItem(labelOf(variant), icon);
      item.addActionListener(e -> choose(variant));
      menu.add(item);
    }
    menu.show(this, getWidth(), 0);
  }

  private void choose(RelationVariant variant) {
    Preferences.userNodeForPackage(RelationVariantButton.class).put(preferenceKey, variant.type.name());
    show(variant);
    chosen.accept(variant.type);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    Color color = UIManager.getColor("Label.foreground");
    g2.setColor(color != null ? color : Color.BLACK);
    int right = getWidth() - ARROW_MARGIN;
    int bottom = getHeight() - ARROW_MARGIN;
    g2.fillPolygon(new int[] {right - ARROW_WIDTH, right, right - ARROW_WIDTH / 2},
      new int[] {bottom - ARROW_HEIGHT, bottom - ARROW_HEIGHT, bottom}, 3);
    g2.dispose();
  }
}
