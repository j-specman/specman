package classman.editor;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ClassStyleModel_V001;
import classman.model.v001.TextChangeModel_V001;
import classman.model.v001.Visibility_V001;

import java.util.ArrayList;

/** The diagram which the style dialog shows: one class with three attributes. With a change set, one attribute has a
 * changed type, one is added and one is removed. */
final class StylePreviewModel {

  private StylePreviewModel() {
  }

  /** @param changeSet the change set which marks the attributes, null for no marks
   * @return a diagram with the class Person, which has a copy of the style. Name and attributes are the same in every
   * language, so the preview needs no texts of its own. */
  static ClassDiagramModel_V001 of(ClassStyleModel_V001 style, String changeSet) {
    ClassModel_V001 box = new ClassModel_V001("Person");
    box.style = style.copy();
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
