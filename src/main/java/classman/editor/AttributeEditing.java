package classman.editor;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.change.AttributeChanger_V001;
import classman.model.v001.change.AttributeLineFormat_V001;
import classman.model.v001.change.AttributeLine_V001;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Changes, adds and deletes the attributes of the classes of a diagram, see {@link MemberEditing}. */
class AttributeEditing extends MemberEditing<AttributeModel_V001, AttributeLine_V001> {
  private final AttributeLineFormat_V001 format = new AttributeLineFormat_V001();

  AttributeEditing(ClassDiagramModel_V001 model) {
    super(model, AttributeModel_V001.class, new AttributeChanger_V001());
  }

  @Override
  protected List<AttributeModel_V001> membersOf(ClassModel_V001 owner) {
    return owner.attributes;
  }

  @Override
  protected AttributeModel_V001 blank() {
    return new AttributeModel_V001(null, "", null, new ArrayList<>());
  }

  @Override
  protected String format(AttributeModel_V001 attribute) {
    return format.textOf(attribute);
  }

  /** The line of an attribute, or of a value of an enum, which has no type and so no colon. */
  @Override
  protected Optional<AttributeLine_V001> parse(ClassModel_V001 owner, String text) {
    return format.parse(text, owner.kind.hasTypedAttributes());
  }
}
