package classman.editor;

import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.StyleProperty_V001;
import classman.render.DiagramRenderer;
import classman.render.Scene;

import java.awt.Window;
import java.util.List;

/** The style dialog of a class, enum or interface. Its preview is a class with three attributes, and the stereotype of
 * the class, so that the colors of the configuration which the stereotype may refer to show in the preview and are what
 * "default" means in the dialog. */
class ClassStyleDialog extends ElementStyleDialog {
  private final String stereotype;

  /** @param stereotype the stereotype of the class, null if it has none */
  ClassStyleDialog(Window owner, ElementStyleModel_V001 start, DiagramRenderer renderer, List<String> changeSets,
                   EditorTexts texts, String stereotype) {
    super(owner, start, renderer, changeSets, texts, "style.dialog.title.class", StyleProperty_V001.ALL,
      StandardColors.forClass(renderer.stereotypeLookOf(stereotype)));
    this.stereotype = stereotype;
  }

  @Override
  protected Scene previewScene(ElementStyleModel_V001 previewStyle, String changeSet) {
    return ClassPreview.scene(renderer, previewStyle, changeSet, stereotype);
  }
}
