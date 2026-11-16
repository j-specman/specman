package specman.editarea.image.classman;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import specman.editarea.image.SourceValidator;

import static specman.i18n.Messages.i18n;

/** The rules Classman diagram source must follow to be accepted by Specman. A diagram without content is rendered as
 * a tiny empty picture, which the user could hardly find and click again. */
public class ClassmanSourceValidator implements SourceValidator {
  public static final String BACKTICK_MESSAGE_KEY = "msg.classman.backtick";
  public static final String EMPTY_MESSAGE_KEY = "msg.classman.empty";

  /** @return a message telling the user what is wrong with the source, or null if it is fine */
  @Override
  public String problemOf(String source) {
    if (source.indexOf('`') >= 0) {
      return i18n(BACKTICK_MESSAGE_KEY);
    }
    try {
      ClassDiagramModel_V001 model = ClassmanRenderer.modelOf(source);
      if (model.elements.isEmpty()) {
        return i18n(EMPTY_MESSAGE_KEY);
      }
      ClassmanRenderer.sceneOf(model);
      return null;
    }
    catch (ClassDiagramParseException | IllegalArgumentException problem) {
      return problem.getMessage();
    }
  }
}
