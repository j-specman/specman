package specman.visitor;

import specman.editarea.EditContainer;
import specman.editarea.image.AbstractImageEditArea;
import specman.view.AbstractSchrittView;

import java.util.ArrayList;
import java.util.List;

import static specman.Specman.editor;
import static specman.i18n.Messages.i18n;

/** Lists where a meta tag configuration is used in the traversed part of the document, for the user who is about to
 * delete it, in the order of the document: a step with a tag is named by its number, an element of a diagram, e.g. a
 * class, by its name and where the diagram is, the number of its step or the intro or outro. */
public class MetaTagConfigUseLister implements DiagramVisitor {
  private final String configName;
  private final List<String> uses = new ArrayList<>();
  private String location = "";

  public MetaTagConfigUseLister(String configName) {
    this.configName = configName;
  }

  @Override
  public void visitStep(AbstractSchrittView step) {
    location = i18n("dialog.metatag.configs.use.step", step.getNumber().toString());
    if (step.getTagConfigNames().contains(configName)) {
      uses.add(location);
    }
  }

  @Override
  public void visitEditContainer(EditContainer editContainer) {
    if (editContainer == editor().getIntro()) {
      location = i18n("dialog.metatag.configs.use.intro");
    }
    else if (editContainer == editor().getOutro()) {
      location = i18n("dialog.metatag.configs.use.outro");
    }
  }

  @Override
  public void visitImageArea(AbstractImageEditArea imageArea) {
    for (String elementName : imageArea.metaTagConfigUserNames(configName)) {
      uses.add(i18n("dialog.metatag.configs.use.element", elementName, location));
    }
  }

  public List<String> uses() {
    return uses;
  }
}
