package classman.editor;

import classman.model.v001.ClassModel_V001;

/** The blank row for a new stereotype which is put into a class while the stereotype is typed. It is part of the
 * picture, not of the diagram: it is taken away again before anything is written, and when it is dropped. */
class PendingStereotype {
  private ClassModel_V001 owner;

  /** Puts the blank row into the class, which must not have a stereotype. */
  void begin(ClassModel_V001 box) {
    discard();
    box.stereotype = "";
    owner = box;
  }

  /** Takes the blank row away again if it is still there.
   * @return true if there was one */
  boolean discard() {
    if (owner == null) {
      return false;
    }
    owner.stereotype = null;
    owner = null;
    return true;
  }
}
