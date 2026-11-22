package classman.editor;

import classman.model.v001.ClassKind_V001;
import classman.model.v001.ClassModel_V001;
import classman.render.DiagramStyle;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.util.function.UnaryOperator;

/** The menu of a class: it adds the parts which a class can have, each typed into its place in the class right away,
 * gives the box a color of its own, shows or hides its attribute compartment, and switches back to the width which
 * follows the content. An entry which doesn't fit the class right now, like a second stereotype, is there but can't be
 * chosen. */
class ClassContextMenu extends JPopupMenu {

  /** @param colorPicker asks the user for a color, given the one to start from, and gives null if the user doesn't
   * choose one
   * @param showDiagram shows the diagram as the editing has it now */
  ClassContextMenu(DiagramEditing editing, ClassModel_V001 box, TextEditor textEditor, EditorTexts texts,
                   UnaryOperator<Color> colorPicker, Runnable showDiagram) {
    boolean isEnum = box.kind == ClassKind_V001.ENUM;
    add(entry(texts.get("menu.class.add.stereotype"), editing.canAddStereotypeTo(box),
      () -> textEditor.startNewStereotype(box)));
    add(entry(texts.get("menu.class.add.taggedvalue"), editing.canAddPartsTo(box),
      () -> textEditor.startNewTaggedValue(box)));
    add(entry(texts.get(isEnum ? "menu.class.add.value" : "menu.class.add.attribute"), editing.canAddAttributeTo(box),
      () -> textEditor.startNewAttribute(box)));
    addSeparator();
    add(entry(texts.get("menu.class.color"), editing.canChangeFill(box), () -> chooseColor(editing, box, colorPicker,
      showDiagram)));
    add(entry(texts.get("menu.class.color.standard"), editing.canChangeFill(box) && box.fill != null, () -> {
      editing.setFill(box, null);
      showDiagram.run();
    }));
    JCheckBoxMenuItem showsAttributes = new JCheckBoxMenuItem(
      texts.get(isEnum ? "menu.class.show.values" : "menu.class.show.attributes"), box.showsAttributes());
    showsAttributes.setEnabled(editing.canHideAttributesOf(box));
    showsAttributes.addActionListener(e -> {
      editing.setAttributesHidden(box, box.showsAttributes());
      showDiagram.run();
    });
    add(showsAttributes);
    addSeparator();
    add(new AutomaticWidthMenuItem(editing, box, texts, showDiagram));
  }

  private static void chooseColor(DiagramEditing editing, ClassModel_V001 box, UnaryOperator<Color> colorPicker,
                                  Runnable showDiagram) {
    Color start = Color.decode(box.fill != null ? box.fill : DiagramStyle.CLASS_FILL);
    Color chosen = colorPicker.apply(start);
    if (chosen != null) {
      editing.setFill(box, String.format("#%02x%02x%02x", chosen.getRed(), chosen.getGreen(), chosen.getBlue()));
      showDiagram.run();
    }
  }

  private static JMenuItem entry(String label, boolean enabled, Runnable action) {
    JMenuItem item = new JMenuItem(label);
    item.setEnabled(enabled);
    item.addActionListener(e -> action.run());
    return item;
  }
}
