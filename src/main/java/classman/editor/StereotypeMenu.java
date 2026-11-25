package classman.editor;

import classman.model.v001.ClassModel_V001;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

/** The submenu of a class for its stereotype: free text, which is typed into the class, then one entry for each
 * configuration of the host, then removing the stereotype. The entry of the kind the stereotype has is selected. */
class StereotypeMenu extends JMenu {
  private final DiagramEditing editing;
  private final ClassModel_V001 box;
  private final Runnable showDiagram;

  StereotypeMenu(DiagramEditing editing, ClassModel_V001 box, TextEditor textEditor, EditorTexts texts,
                 Runnable showDiagram) {
    super(texts.get("menu.class.stereotype"));
    this.editing = editing;
    this.box = box;
    this.showDiagram = showDiagram;
    editing.canChangeStereotypeOf(box).applyTo(this);
    ButtonGroup kinds = new ButtonGroup();
    JRadioButtonMenuItem freeText = new JRadioButtonMenuItem(texts.get("menu.class.stereotype.freetext"),
      editing.hasFreeTextStereotype(box));
    freeText.addActionListener(e -> textEditor.startFreeTextStereotype(box));
    kinds.add(freeText);
    add(freeText);
    addSeparator();
    for (String configName : editing.stereotypeConfigs()) {
      JRadioButtonMenuItem item = configItem(configName);
      kinds.add(item);
      add(item);
    }
    if (!editing.stereotypeConfigs().isEmpty()) {
      addSeparator();
    }
    add(removeItem(texts));
  }

  private JRadioButtonMenuItem configItem(String configName) {
    JRadioButtonMenuItem item = new JRadioButtonMenuItem(configName, configName.equals(editing.stereotypeConfigOf(box)));
    item.addActionListener(e -> {
      editing.setStereotypeConfig(box, configName);
      showDiagram.run();
    });
    return item;
  }

  private JMenuItem removeItem(EditorTexts texts) {
    JMenuItem item = new JMenuItem(texts.get("menu.class.stereotype.remove"));
    editing.canRemoveStereotypeOf(box).applyTo(item);
    item.addActionListener(e -> {
      editing.removeStereotype(box);
      showDiagram.run();
    });
    return item;
  }
}
