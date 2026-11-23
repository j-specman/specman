package classman.editor;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.ElementStyleModel_V001;

import javax.swing.JMenuItem;
import java.util.function.UnaryOperator;

/** The menu entry which opens the style dialog of a class, note or relation and gives the element the style which the
 * user has chosen. It can't be chosen while the element can't get a style, and its tooltip says why. */
class StyleMenuEntry extends JMenuItem {

  /** @param stylePicker asks the user for a style, given a copy of the one to start from, and gives null if the user
   * doesn't choose one
   * @param showDiagram shows the diagram as the editing has it now */
  StyleMenuEntry(DiagramEditing editing, AbstractDiagramElementModel_V001 element, EditorTexts texts,
                 UnaryOperator<ElementStyleModel_V001> stylePicker, Runnable showDiagram) {
    super(texts.get("menu.style"));
    editing.canChangeStyle(element).applyTo(this);
    addActionListener(e -> {
      ElementStyleModel_V001 chosen = stylePicker.apply(element.style.copy());
      if (chosen != null) {
        editing.setStyle(element, chosen);
        showDiagram.run();
      }
    });
  }
}
