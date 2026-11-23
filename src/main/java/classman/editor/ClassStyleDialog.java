package classman.editor;

import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.StyleProperty_V001;
import classman.render.DiagramRenderer;
import classman.render.DiagramStyle;
import classman.render.Scene;

import java.awt.Window;
import java.util.List;

/** The style dialog of a class, enum or interface. Its preview is a class with three attributes. */
class ClassStyleDialog extends ElementStyleDialog {

  ClassStyleDialog(Window owner, ElementStyleModel_V001 start, DiagramRenderer renderer, List<String> changeSets,
                   EditorTexts texts) {
    super(owner, start, renderer, changeSets, texts, "style.dialog.title.class", StyleProperty_V001.ALL,
      DiagramStyle.CLASS_FILL);
  }

  @Override
  protected Scene previewScene(ElementStyleModel_V001 previewStyle, String changeSet) {
    return ClassPreview.scene(renderer, previewStyle, changeSet);
  }
}
