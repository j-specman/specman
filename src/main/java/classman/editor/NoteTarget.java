package classman.editor;

import classman.model.v001.NoteModel_V001;

import java.awt.geom.Rectangle2D;
import java.util.function.Supplier;

/** A note as the start of a connection. */
class NoteTarget extends ConnectionTarget {
  final NoteModel_V001 note;
  private final Rectangle2D area;

  NoteTarget(NoteModel_V001 note, Rectangle2D area) {
    this.note = note;
    this.area = area;
  }

  @Override
  Object element() { return note; }

  @Override
  Rectangle2D frame() { return area; }

  @Override
  String ensureId(Supplier<String> newId) {
    if (note.id == null) {
      note.id = newId.get();
    }
    return note.id;
  }
}
