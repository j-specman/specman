package classman.render;

import classman.model.v001.TextPlaceModel_V001;

/** A text of a relation to be placed, and the place where the user has put it, null if the layout decides. */
final class RelationText {
    /** Null or empty if the relation hasn't got the text. */
    final String text;
    final TextPlaceModel_V001 place;

    RelationText(String text, TextPlaceModel_V001 place) {
        this.text = text;
        this.place = place;
    }

    boolean isMissing() {
        return text == null || text.isEmpty();
    }
}
