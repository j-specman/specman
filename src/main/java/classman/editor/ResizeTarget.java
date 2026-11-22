package classman.editor;

import classman.model.v001.ClassModel_V001;

/** A side of a class which the mouse is at, where dragging changes the width of the class. */
class ResizeTarget {
  final ClassModel_V001 box;
  final ResizeSide side;

  ResizeTarget(ClassModel_V001 box, ResizeSide side) {
    this.box = box;
    this.side = side;
  }
}
