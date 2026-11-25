package classman.editor;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.TextChangeModel_V001;
import classman.model.v001.Visibility_V001;
import classman.render.ClassBoxLayouter;
import classman.render.DiagramRenderer;
import classman.render.Scene;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/** The diagram which the style dialog of a class shows: one class with three attributes. With a change set, one
 * attribute has a changed type, one is added and one is removed. */
final class ClassPreview {

  private ClassPreview() {
  }

  /** @return the picture of the class in the style. With a change set it has the frame of an added class, and its
   * attributes are marked, which a real diagram never shows together: the frame stands for everything in the class.
   * Here both are shown, so that the user sees all marks next to the colors of the style. */
  static Scene scene(DiagramRenderer renderer, ElementStyleModel_V001 style, String changeSet, String stereotype) {
    Scene laidOut = renderer.layout(of(style, changeSet, stereotype));
    if (changeSet == null) {
      return laidOut;
    }
    Rectangle2D.Double area = laidOut.elementAreas.get(0).area;
    return FramedScene.of(laidOut,
      ClassBoxLayouter.frameOf(area.width, area.height, renderer.strongColorOf(changeSet)), area);
  }

  /** @param changeSet the change set which marks the attributes, null for no marks
   * @param stereotype the stereotype of the class which the style is for, null if it has none
   * @return a diagram with the class Person, which has a copy of the style and the stereotype. Name and attributes are the same in every
   * language, so the preview needs no texts of its own. */
  private static ClassDiagramModel_V001 of(ElementStyleModel_V001 style, String changeSet, String stereotype) {
    ClassModel_V001 box = new ClassModel_V001("Person");
    box.style = style.copy();
    box.stereotype = stereotype;
    AttributeModel_V001 changed = attribute("name", "String");
    AttributeModel_V001 added = attribute("email", "String");
    AttributeModel_V001 removed = attribute("fon", "String");
    if (changeSet != null) {
      changed.typeChange = new TextChangeModel_V001(changeSet, "Text");
      added.change = new ChangeModel_V001(ChangeType_V001.ADDED, changeSet);
      removed.change = new ChangeModel_V001(ChangeType_V001.REMOVED, changeSet);
    }
    box.attributes.add(changed);
    box.attributes.add(added);
    box.attributes.add(removed);
    ClassDiagramModel_V001 diagram = new ClassDiagramModel_V001(null);
    diagram.elements.add(box);
    return diagram;
  }

  private static AttributeModel_V001 attribute(String name, String type) {
    return new AttributeModel_V001(Visibility_V001.PUBLIC, name, type, new ArrayList<>());
  }
}
