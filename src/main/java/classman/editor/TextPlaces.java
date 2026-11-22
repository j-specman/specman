package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.TextPlaceModel_V001;
import classman.render.TextKind;

/** The places which the user has put the texts of a relation at: the label, and the multiplicities of an association,
 * aggregation or composition. */
final class TextPlaces {
  private TextPlaces() {}

  /** @return true if the user can move this kind of text of the relation */
  static boolean isMovable(AbstractRelationModel_V001 relation, TextKind kind) {
    boolean multiplicity = kind == TextKind.FROM_MULTIPLICITY || kind == TextKind.TO_MULTIPLICITY;
    return kind == TextKind.RELATION_LABEL || multiplicity && relation instanceof AssociationRelationModel_V001;
  }

  /** @return the place of the text, null if the layout places it */
  static TextPlaceModel_V001 of(AbstractRelationModel_V001 relation, TextKind kind) {
    return switch (kind) {
      case RELATION_LABEL -> relation.labelPlace;
      case FROM_MULTIPLICITY -> ((AssociationRelationModel_V001) relation).fromMultiplicityPlace;
      case TO_MULTIPLICITY -> ((AssociationRelationModel_V001) relation).toMultiplicityPlace;
      default -> throw new IllegalArgumentException("The user can't move a text of the kind " + kind);
    };
  }

  /** Puts the text at the place, null gives it back to the layout. */
  static void set(AbstractRelationModel_V001 relation, TextKind kind, TextPlaceModel_V001 place) {
    switch (kind) {
      case RELATION_LABEL -> relation.labelPlace = place;
      case FROM_MULTIPLICITY -> ((AssociationRelationModel_V001) relation).fromMultiplicityPlace = place;
      case TO_MULTIPLICITY -> ((AssociationRelationModel_V001) relation).toMultiplicityPlace = place;
      default -> throw new IllegalArgumentException("The user can't move a text of the kind " + kind);
    }
  }
}
