package classman.editor;

import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RelationType_V001;
import classman.render.DiagramRenderer;
import classman.render.Scene;

/** The diagram which the style dialog of a relation shows: two classes and an association between them, with an
 * arrowhead, a label and a multiplicity at each end. With a change set the relation is added, which marks its line and
 * its label. */
final class RelationPreview {
  private static final String FROM_ID = "a0000001";
  private static final String TO_ID = "b0000002";
  private static final int DISTANCE = 170;

  private RelationPreview() {
  }

  /** @return the picture of the relation in the style, which a change set has added if there is one */
  static Scene scene(DiagramRenderer renderer, ElementStyleModel_V001 style, String changeSet) {
    return renderer.layout(of(style, changeSet));
  }

  /** @param changeSet the change set which added the relation, null for none
   * @return a diagram with the classes A and B and a relation which has a copy of the style. The texts are the same in
   * every language, so the preview needs no texts of its own. */
  private static ClassDiagramModel_V001 of(ElementStyleModel_V001 style, String changeSet) {
    ClassDiagramModel_V001 diagram = new ClassDiagramModel_V001(null);
    diagram.elements.add(box("A", FROM_ID, 0));
    diagram.elements.add(box("B", TO_ID, DISTANCE));
    AssociationRelationModel_V001 relation =
      new AssociationRelationModel_V001(RelationType_V001.ASSOCIATION, FROM_ID, TO_ID);
    relation.style = style.copy();
    relation.toArrow = true;
    relation.fromMultiplicity = "1";
    relation.toMultiplicity = "0..*";
    relation.label = "label";
    if (changeSet != null) {
      relation.change = new ChangeModel_V001(ChangeType_V001.ADDED, changeSet);
    }
    diagram.elements.add(relation);
    return diagram;
  }

  private static ClassModel_V001 box(String name, String id, int x) {
    ClassModel_V001 box = new ClassModel_V001(name);
    box.id = id;
    box.position = new PositionModel_V001(x, 0);
    return box;
  }
}
