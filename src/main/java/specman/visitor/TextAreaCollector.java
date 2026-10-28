package specman.visitor;

import specman.editarea.TextEditArea;

import java.util.ArrayList;
import java.util.List;

/** Collects all text areas of the traversed part of the diagram in document order. For the whole diagram: Intro, the areas of the steps, outro. */
public class TextAreaCollector implements DiagramVisitor {

  private final List<TextEditArea> textAreas = new ArrayList<>();

  @Override
  public void visitTextArea(TextEditArea textArea) {
    textAreas.add(textArea);
  }

  /** @return the text areas collected so far, a modifiable list the caller owns */
  public List<TextEditArea> textAreas() {
    return textAreas;
  }
}
