package classman.editor;

import classman.model.v001.ClassModel_V001;

/** The blank row for a new stereotype which is put into a class while the stereotype is typed. It is part of the
 * picture, not of the diagram: it is taken away again before anything is written, and when it is dropped. A class
 * which has a stereotype already can be given a blank row too, which takes the place of the stereotype until the row
 * is done with. */
class PendingStereotype {
  private ClassModel_V001 owner;
  private String replaced;

  /** Puts the blank row into the class, which must not have a stereotype. */
  void begin(ClassModel_V001 box) {
    replace(box);
  }

  /** Puts the blank row in the place of the stereotype of the class, which comes back when the row is taken away. */
  void replace(ClassModel_V001 box) {
    discard();
    replaced = box.stereotype;
    box.stereotype = "";
    owner = box;
  }

  /** Takes the blank row away again if it is still there, and gives the class back the stereotype which it replaced,
   * if any.
   * @return true if there was one */
  boolean discard() {
    if (owner == null) {
      return false;
    }
    owner.stereotype = replaced;
    owner = null;
    replaced = null;
    return true;
  }
}
