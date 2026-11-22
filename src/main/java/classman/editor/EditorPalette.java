package classman.editor;

import classman.model.v001.ClassKind_V001;
import classman.model.v001.RelationType_V001;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** The vertical tool bar at the left edge of the editor window. Buttons show an icon and a text, or an icon only,
 * which the button at the bottom switches, and which is remembered while the application runs. The most important element comes first, at the top. A class or a note is
 * dragged from its button onto the diagram, the button of a tool for relations is switched on and then used on the
 * diagram. The relation buttons have variants, see {@link VariantButton}. */
class EditorPalette extends JToolBar {
  private static final float TEXT_SCALE = 1.2f;
  private static final int ICON_TEXT_GAP = 8;
  /** Whether the buttons show a text, for the palettes which are made from now on: what the user switched to last, as
   * long as the application runs. Text at first. */
  private static boolean showTextInNewPalettes = true;

  private final EditorTexts texts;
  private final List<AbstractButton> elementButtons = new ArrayList<>();
  private final List<VariantButton<RelationType_V001>> relationButtons = new ArrayList<>();
  private final JToggleButton anchorToolButton = new JToggleButton();
  private final JToggleButton assocClassToolButton = new JToggleButton();
  private final JButton toggleButton = new JButton();
  private final Map<AbstractButton, String> labels = new HashMap<>();
  private final Map<AbstractButton, String> tooltips = new HashMap<>();
  private boolean showText;

  /** @param classDropped gets the kind of the class which the class button creates, and the point on the screen where
   * it was released
   * @param noteDropped gets the point on the screen where the note button was released
   * @param relationToolToggled is run with the type of the relation when the button for drawing relations is pressed
   * @param relationTypeChosen is run with the type of the relation which the user chose in the menu of a button
   * @param anchorToolToggled is run when the button for drawing the anchor of a note is pressed
   * @param assocClassToolToggled is run when the button for drawing the link of an association class is pressed */
  EditorPalette(EditorTexts texts, BiConsumer<ClassKind_V001, Point> classDropped, Consumer<Point> noteDropped,
                Consumer<RelationType_V001> relationToolToggled, Consumer<RelationType_V001> relationTypeChosen,
                Runnable anchorToolToggled, Runnable assocClassToolToggled) {
    super(JToolBar.VERTICAL);
    this.texts = texts;
    this.showText = showTextInNewPalettes;
    setFloatable(false);

    toggleButton.setMargin(new Insets(0, 0, 0, 0));
    toggleButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    toggleButton.addActionListener(e -> toggleShowText());

    addClassButton(classDropped);
    addDragButton("note", EditorIcons.NOTE, noteDropped);
    addRelationButton("variantAssociation", relationToolToggled, relationTypeChosen,
      new PaletteVariant<>("association", RelationType_V001.ASSOCIATION, EditorIcons.ASSOCIATION),
      new PaletteVariant<>("aggregation", RelationType_V001.AGGREGATION, EditorIcons.AGGREGATION),
      new PaletteVariant<>("composition", RelationType_V001.COMPOSITION, EditorIcons.COMPOSITION));
    addRelationButton("variantInheritance", relationToolToggled, relationTypeChosen,
      new PaletteVariant<>("extends", RelationType_V001.EXTENDS, EditorIcons.EXTENDS),
      new PaletteVariant<>("implements", RelationType_V001.IMPLEMENTS, EditorIcons.IMPLEMENTS));
    addToolButton("anchor", EditorIcons.ANCHOR, anchorToolButton, anchorToolToggled);
    addToolButton("assocclass", EditorIcons.ASSOC_CLASS, assocClassToolButton, assocClassToolToggled);

    add(Box.createVerticalGlue());
    addSeparator();
    add(toggleButton);
    applyShowText();
  }

  private void addDragButton(String key, String iconPath, Consumer<Point> dropped) {
    JButton button = new JButton();
    Icon icon = EditorIcons.read(iconPath, EditorIcons.SIZE);
    PaletteDragAdapter dragAdapter = new PaletteDragAdapter(() -> texts.get("palette.label." + key), () -> icon,
      dropped);
    button.addMouseListener(dragAdapter);
    button.addMouseMotionListener(dragAdapter);
    addElementButton(button, key, icon);
  }

  private void addClassButton(BiConsumer<ClassKind_V001, Point> dropped) {
    VariantButton<ClassKind_V001> button = new VariantButton<>(texts, List.of(
      new PaletteVariant<>("class", ClassKind_V001.CLASS, EditorIcons.CLASS),
      new PaletteVariant<>("interface", ClassKind_V001.INTERFACE, EditorIcons.INTERFACE),
      new PaletteVariant<>("enum", ClassKind_V001.ENUM, EditorIcons.ENUM)), "variantClass", kind -> { });
    PaletteDragAdapter dragAdapter = new PaletteDragAdapter(button::currentLabel,
      () -> EditorIcons.read(button.currentIconPath(), EditorIcons.SIZE),
      screenPoint -> dropped.accept(button.currentValue(), screenPoint));
    button.addMouseListener(dragAdapter);
    button.addMouseMotionListener(dragAdapter);
    styleElementButton(button);
    add(button);
  }

  private void addToolButton(String key, String iconPath, JToggleButton button, Runnable toggled) {
    button.addActionListener(e -> toggled.run());
    addElementButton(button, key, EditorIcons.read(iconPath, EditorIcons.SIZE));
  }

  private void addRelationButton(String buttonName, Consumer<RelationType_V001> toggled,
                                 Consumer<RelationType_V001> chosen, PaletteVariant<RelationType_V001>... variants) {
    VariantButton<RelationType_V001> button = new VariantButton<>(texts, List.of(variants), buttonName, chosen);
    button.addActionListener(e -> toggled.accept(button.currentValue()));
    styleElementButton(button);
    relationButtons.add(button);
    add(button);
  }

  /** @param key tells the texts of the button: palette.label.key and palette.create.key */
  private void addElementButton(AbstractButton button, String key, Icon icon) {
    button.setIcon(icon);
    styleElementButton(button);
    labels.put(button, texts.get("palette.label." + key));
    tooltips.put(button, texts.get("palette.create." + key));
    add(button);
  }

  private void styleElementButton(AbstractButton button) {
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setFont(button.getFont().deriveFont(button.getFont().getSize2D() * TEXT_SCALE));
    button.setIconTextGap(ICON_TEXT_GAP);
    button.setHorizontalAlignment(SwingConstants.LEADING);
    button.setHorizontalTextPosition(SwingConstants.TRAILING);
    button.setAlignmentX(Component.LEFT_ALIGNMENT);
    elementButtons.add(button);
  }

  /** Shows which tool is armed, if one is, as a pressed button. The button of a relation shows the type which is armed.
   * @param armedRelationType null if the tool for relations isn't armed */
  void showToolsArmed(RelationType_V001 armedRelationType, boolean isAnchorToolArmed,
                      boolean isAssocClassToolArmed) {
    for (VariantButton<RelationType_V001> button : relationButtons) {
      boolean isArmed = armedRelationType != null && button.offers(armedRelationType);
      if (isArmed) {
        button.showValue(armedRelationType);
      }
      button.setSelected(isArmed);
    }
    anchorToolButton.setSelected(isAnchorToolArmed);
    assocClassToolButton.setSelected(isAssocClassToolArmed);
  }

  private void toggleShowText() {
    showText = !showText;
    showTextInNewPalettes = showText;
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
    for (AbstractButton button : elementButtons) {
      if (button instanceof VariantButton<?> variantButton) {
        variantButton.showText(showText);
      }
      else {
        button.setText(showText ? labels.get(button) : null);
        button.setToolTipText(showText ? null : tooltips.get(button));
      }
      button.setPreferredSize(null);
      button.setMaximumSize(null);
    }
    toggleButton.setMaximumSize(null);
    equalizeButtonWidths();
  }

  /** Gives all buttons, including the toggle button, the width of the widest one. */
  private void equalizeButtonWidths() {
    int maxWidth = elementButtons.stream().mapToInt(b -> b.getPreferredSize().width).max().orElse(0);
    for (AbstractButton button : elementButtons) {
      button.setPreferredSize(new Dimension(maxWidth, button.getPreferredSize().height));
      button.setMaximumSize(button.getPreferredSize());
    }
    toggleButton.setMaximumSize(new Dimension(maxWidth, toggleButton.getPreferredSize().height));
  }
}
