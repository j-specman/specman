package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;

/** A side of a class or note which the mouse is at, where dragging changes the width of the box. */
class ResizeTarget {
  final AbstractBoxModel_V001 box;
  final ResizeSide side;

  ResizeTarget(AbstractBoxModel_V001 box, ResizeSide side) {
    this.box = box;
    this.side = side;
  }
}
