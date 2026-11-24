package classman.editor;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.MethodModel_V001;
import classman.model.v001.change.MethodChanger_V001;
import classman.model.v001.change.MethodLineFormat_V001;
import classman.model.v001.change.MethodLine_V001;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Changes, adds and deletes the methods of the classes of a diagram, see {@link MemberEditing}. */
class MethodEditing extends MemberEditing<MethodModel_V001, MethodLine_V001> {
  private final MethodLineFormat_V001 format = new MethodLineFormat_V001();

  MethodEditing(ClassDiagramModel_V001 model) {
    super(model, MethodModel_V001.class, new MethodChanger_V001());
  }

  @Override
  protected List<MethodModel_V001> membersOf(ClassModel_V001 owner) {
    return owner.methods;
  }

  @Override
  protected MethodModel_V001 blank() {
    return new MethodModel_V001(null, "", new ArrayList<>(), null, new ArrayList<>());
  }

  @Override
  protected String format(MethodModel_V001 method) {
    return format.textOf(method);
  }

  @Override
  protected Optional<MethodLine_V001> parse(ClassModel_V001 owner, String text) {
    return format.parse(text);
  }
}
