package classman.editor;

import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.StyleProperty_V001;
import classman.render.DiagramRenderer;
import classman.render.Scene;

import java.awt.Window;
import java.util.List;

/** The style dialog of a relation, which has no background. Its preview is a relation between two classes. */
class RelationStyleDialog extends ElementStyleDialog {

  RelationStyleDialog(Window owner, ElementStyleModel_V001 start, DiagramRenderer renderer, List<String> changeSets,
                      EditorTexts texts) {
    super(owner, start, renderer, changeSets, texts, "style.dialog.title.relation", StyleProperty_V001.WITHOUT_FILL,
      StandardColors.RELATION);
  }

  @Override
  protected Scene previewScene(ElementStyleModel_V001 previewStyle, String changeSet) {
    return RelationPreview.scene(renderer, previewStyle, changeSet);
  }
}
