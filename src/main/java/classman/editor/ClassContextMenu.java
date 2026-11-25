package classman.editor;

import classman.model.v001.AbstractMemberModel_V001;
import classman.model.v001.ClassKind_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.Compartment_V001;
import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.MethodModel_V001;
import classman.model.v001.change.MethodLineFormat_V001;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.function.UnaryOperator;

/** The menu of a class: it adds the parts which a class can have, each typed into its place in the class right away,
 * shows and hides single attributes and methods by submenus of check items, which stay open while the user checks
 * them. It opens the dialog for the style of the box (colors, font, font size), shows or hides its compartments, and
 * switches back to the width which follows the content. An entry which doesn't fit the class right now, like a second
 * stereotype, is there but can't be chosen, and its tooltip says why. */
class ClassContextMenu extends JPopupMenu {

  /** @param stylePicker asks the user for a style, given a copy of the one to start from, and gives null if the user
   * doesn't choose one
   * @param showDiagram shows the diagram as the editing has it now */
  ClassContextMenu(DiagramEditing editing, ClassModel_V001 box, TextEditor textEditor, EditorTexts texts,
                   UnaryOperator<ElementStyleModel_V001> stylePicker, Runnable showDiagram) {
    add(new StereotypeMenu(editing, box, textEditor, texts, showDiagram));
    add(entry(texts.get("menu.class.add.taggedvalue"), editing.canAddPartsTo(box),
      () -> textEditor.startNewTaggedValue(box)));
    for (Compartment_V001 compartment : Compartment_V001.values()) {
      add(entry(texts.get("menu.class.add." + word(box, compartment)), editing.canAddMemberTo(box, compartment),
        () -> textEditor.startNewMember(box, compartment)));
    }
    for (Compartment_V001 compartment : Compartment_V001.values()) {
      add(visibleMembersMenu(editing, box, compartment, texts, showDiagram));
    }
    addSeparator();
    add(new StyleMenuEntry(editing, box, texts, stylePicker, showDiagram));
    addSeparator();
    add(new AutomaticWidthMenuItem(editing, box, texts, showDiagram));
  }

  /** What the members of the compartment are called for the user: attributes, the values of an enum, methods. */
  private static String word(ClassModel_V001 box, Compartment_V001 compartment) {
    if (compartment == Compartment_V001.METHODS) {
      return "method";
    }
    return box.kind == ClassKind_V001.ENUM ? "value" : "attribute";
  }

  private static JCheckBoxMenuItem showsCompartmentItem(DiagramEditing editing, ClassModel_V001 box,
                                                        Compartment_V001 compartment, EditorTexts texts,
                                                        Runnable showDiagram) {
    JCheckBoxMenuItem item = new JCheckBoxMenuItem(texts.get("menu.class.show." + word(box, compartment) + "s"),
      compartment.isShownIn(box));
    editing.canHideCompartmentOf(box, compartment).applyTo(item);
    item.addActionListener(e -> {
      editing.setCompartmentHidden(box, compartment, compartment.isShownIn(box));
      showDiagram.run();
    });
    return item;
  }

  /** The submenu of a compartment: first a check item which shows or hides the whole compartment, then, below a
   * separator, a check item for each attribute or method, checked while it is shown. */
  private static JMenu visibleMembersMenu(DiagramEditing editing, ClassModel_V001 box, Compartment_V001 compartment,
                                          EditorTexts texts, Runnable showDiagram) {
    JMenu menu = new JMenu(texts.get("menu.class.visible." + word(box, compartment)));
    menu.add(showsCompartmentItem(editing, box, compartment, texts, showDiagram));
    boolean separated = false;
    for (AbstractMemberModel_V001 member : box.members(compartment)) {
      if (!member.hiddenMarker) {
        if (!separated) {
          menu.addSeparator();
          separated = true;
        }
        menu.add(visibleItem(editing, box, member, showDiagram));
      }
    }
    return menu;
  }

  private static JCheckBoxMenuItem visibleItem(DiagramEditing editing, ClassModel_V001 box,
                                               AbstractMemberModel_V001 member, Runnable showDiagram) {
    JCheckBoxMenuItem item = new StayOpenCheckBoxMenuItem(label(member), !member.hidden);
    canSwitch(editing, box, member).applyTo(item);
    item.addActionListener(e -> {
      if (member.hidden) {
        editing.showMember(box, member);
      }
      else {
        editing.hideMember(box, member);
      }
      item.setSelected(!member.hidden);
      canSwitch(editing, box, member).applyTo(item);
      showDiagram.run();
    });
    return item;
  }

  /** A method is told from one with the same name and other parameters by its whole line. */
  private static String label(AbstractMemberModel_V001 member) {
    return member instanceof MethodModel_V001 method ? new MethodLineFormat_V001().textOf(method) : member.name;
  }

  private static Can canSwitch(DiagramEditing editing, ClassModel_V001 box, AbstractMemberModel_V001 member) {
    return member.hidden ? editing.canShowMember(box, member) : editing.canHideMember(box, member);
  }

  private static JMenuItem entry(String label, Can can, Runnable action) {
    JMenuItem item = new JMenuItem(label);
    can.applyTo(item);
    item.addActionListener(e -> action.run());
    return item;
  }
}
