package specman.cli.sanitize;

import specman.link.LinkAliases;
import specman.link.classman.ClassmanLink;
import specman.link.classman.ClassmanLinkResolution;
import specman.link.classman.ClassmanLinkResolver;
import specman.model.v002.AbstractEditAreaModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.LinkedClassmanImageEditAreaModel_V002;
import specman.model.v002.io.TextAreaTraversal_V002;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Checks the Classman diagrams of a model which link to a file: every link has to lead to a class diagram, and a file
 * may be linked only once, because two diagrams on one file would overwrite each other's changes. */
public class LinkedDiagramValidator extends AbstractModelValidator {

  private final DiagramModel_V002 model;
  private final ClassmanLinkResolver resolver;

  /** @param resolver follows the links, relative to the document */
  public LinkedDiagramValidator(DiagramModel_V002 model, ClassmanLinkResolver resolver) {
    super("Invalid linked class diagrams");
    this.model = model;
    this.resolver = resolver;
  }

  @Override
  protected void collectErrors(List<String> errors) {
    Map<String, String> firstPositionByFile = new HashMap<>();
    TextAreaTraversal_V002.forEachContent(model, (content, site) -> {
      for (AbstractEditAreaModel_V002 area : content.areas) {
        if (area instanceof LinkedClassmanImageEditAreaModel_V002 linked) {
          checkLink(linked.link, describe(site), firstPositionByFile, errors);
        }
      }
    });
  }

  private void checkLink(String link, String position, Map<String, String> firstPositionByFile, List<String> errors) {
    ClassmanLinkResolution resolution = resolver.resolve(link);
    String linkText = position + ": link '" + link + "' ";
    if (!resolution.isOk()) {
      errors.add(linkText + problemOf(resolution));
      return;
    }
    String firstPosition = firstPositionByFile.putIfAbsent(LinkAliases.absolutePath(resolution.file()), position);
    if (firstPosition != null) {
      errors.add(linkText + "leads to the file " + resolution.file().getPath() + " which " + firstPosition
        + " links as well");
    }
  }

  private static String problemOf(ClassmanLinkResolution resolution) {
    String detail = resolution.problemDetail();
    return switch (resolution.problem()) {
      case NOT_A_LINK -> "is not a Classman link";
      case ALIAS_UNKNOWN -> "names the alias '" + detail + "' which is assigned to no file";
      case WRONG_EXTENSION -> "leads to '" + detail + "' which has not the extension ." + ClassmanLink.FILE_EXTENSION;
      case FILE_MISSING -> "leads to the missing file " + detail;
      case UNREADABLE -> "leads to a file which can't be read: " + detail;
      case NOT_A_DIAGRAM -> "leads to the file " + resolution.file().getPath() + " which is not a valid class diagram: " + detail;
    };
  }
}
