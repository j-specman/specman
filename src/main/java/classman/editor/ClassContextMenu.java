package classman.editor;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassKind_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ElementStyleModel_V001;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.function.UnaryOperator;

/** The menu of a class: it adds the parts which a class can have, each typed into its place in the class right away,
 * shows and hides single attributes by a submenu of check items, which stays open while the user checks them. It
 * opens the dialog for the style of the box (colors, font, font size), shows or hides its attribute compartment, and switches back to the width which follows the content. An entry which
 * doesn't fit the class right now, like a second stereotype, is there but can't be chosen, and its tooltip says why. */
class ClassContextMenu extends JPopupMenu {

  /** @param stylePicker asks the user for a style, given a copy of the one to start from, and gives null if the user
   * doesn't choose one
   * @param showDiagram shows the diagram as the editing has it now */
  ClassContextMenu(DiagramEditing editing, ClassModel_V001 box, TextEditor textEditor, EditorTexts texts,
                   UnaryOperator<ElementStyleModel_V001> stylePicker, Runnable showDiagram) {
    boolean isEnum = box.kind == ClassKind_V001.ENUM;
    add(entry(texts.get("menu.class.add.stereotype"), editing.canAddStereotypeTo(box),
      () -> textEditor.startNewStereotype(box)));
    add(entry(texts.get("menu.class.add.taggedvalue"), editing.canAddPartsTo(box),
      () -> textEditor.startNewTaggedValue(box)));
    add(entry(texts.get(isEnum ? "menu.class.add.value" : "menu.class.add.attribute"), editing.canAddAttributeTo(box),
      () -> textEditor.startNewAttribute(box)));
    add(visibleAttributesMenu(editing, box, texts, isEnum, showDiagram));
    addSeparator();
    add(new StyleMenuEntry(editing, box, texts, stylePicker, showDiagram));
    JCheckBoxMenuItem showsAttributes = new JCheckBoxMenuItem(
      texts.get(isEnum ? "menu.class.show.values" : "menu.class.show.attributes"), box.showsAttributes());
    editing.canHideAttributesOf(box).applyTo(showsAttributes);
    showsAttributes.addActionListener(e -> {
      editing.setAttributesHidden(box, box.showsAttributes());
      showDiagram.run();
    });
    add(showsAttributes);
    addSeparator();
    add(new AutomaticWidthMenuItem(editing, box, texts, showDiagram));
  }

  /** A check item for each attribute, checked while the attribute is shown. The menu can't be opened if the class has
   * no attribute. */
  private static JMenu visibleAttributesMenu(DiagramEditing editing, ClassModel_V001 box, EditorTexts texts,
                                             boolean isEnum, Runnable showDiagram) {
    JMenu menu = new JMenu(texts.get(isEnum ? "menu.class.visible.value" : "menu.class.visible.attribute"));
    for (AttributeModel_V001 attribute : box.attributes) {
      if (!attribute.hiddenAttributesMarker) {
        menu.add(visibleItem(editing, box, attribute, showDiagram));
      }
    }
    menu.setEnabled(menu.getMenuComponentCount() > 0);
    return menu;
  }

  private static JCheckBoxMenuItem visibleItem(DiagramEditing editing, ClassModel_V001 box,
                                               AttributeModel_V001 attribute, Runnable showDiagram) {
    JCheckBoxMenuItem item = new StayOpenCheckBoxMenuItem(attribute.name, !attribute.hidden);
    canSwitch(editing, box, attribute).applyTo(item);
    item.addActionListener(e -> {
      if (attribute.hidden) {
        editing.showAttribute(box, attribute);
      }
      else {
        editing.hideAttribute(box, attribute);
      }
      item.setSelected(!attribute.hidden);
      canSwitch(editing, box, attribute).applyTo(item);
      showDiagram.run();
    });
    return item;
  }

  private static Can canSwitch(DiagramEditing editing, ClassModel_V001 box, AttributeModel_V001 attribute) {
    return attribute.hidden ? editing.canShowAttribute(box, attribute) : editing.canHideAttribute(box, attribute);
  }

  private static JMenuItem entry(String label, Can can, Runnable action) {
    JMenuItem item = new JMenuItem(label);
    can.applyTo(item);
    item.addActionListener(e -> action.run());
    return item;
  }
}
