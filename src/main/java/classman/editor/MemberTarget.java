package classman.editor;

import classman.model.v001.AbstractMemberModel_V001;
import classman.model.v001.ClassModel_V001;

import java.awt.geom.Rectangle2D;
import java.util.function.Supplier;

/** An attribute or a method of a class as the end of a connection. */
class MemberTarget extends ConnectionTarget {
  final ClassModel_V001 owner;
  final AbstractMemberModel_V001 member;
  private final Rectangle2D line;

  MemberTarget(ClassModel_V001 owner, AbstractMemberModel_V001 member, Rectangle2D line) {
    this.owner = owner;
    this.member = member;
    this.line = line;
  }

  @Override
  Object element() { return member; }

  @Override
  Rectangle2D frame() { return line; }

  @Override
  String ensureId(Supplier<String> newId) {
    if (member.id == null) {
      member.id = newId.get();
    }
    return member.id;
  }
}
