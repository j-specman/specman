package classman.editor;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.NoteModel_V001;

/** What is selected while the anchor of a note is selected: not the note, but its dashed line. It stands for the
 * anchor in the selection of the editing, which holds one element, and is no part of the diagram. */
class AnchorSelection extends AbstractDiagramElementModel_V001 {
  final NoteModel_V001 note;

  AnchorSelection(NoteModel_V001 note) {
    this.note = note;
  }
}
