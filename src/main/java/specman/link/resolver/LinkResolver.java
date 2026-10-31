package specman.link.resolver;

import specman.link.LinkAliases;
import specman.link.SpecmanLink;
import specman.link.SpecmanLink.Reference;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.io.ModelFileReader_V002;
import specman.model.v002.io.ModelParseException;
import specman.model.v002.io.ModelRenumberer_V002;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Checks where a link leads to without Specman's user interface, for people and agents who can't ask the running application.
 * It resolves the file the link names or its alias stands for, checks that the file exists and, if the link names a step,
 * that the step is in the file and has the number the link says. The files are checked as they are saved on disk. A resolver
 * keeps the step numbers of the files it has read, so it is worth using one for several links. */
public class LinkResolver {
  private final Map<String, Map<String, String>> stepNumbersByFile = new HashMap<>();

  public LinkResolution resolve(String linkText) {
    Reference reference = SpecmanLink.parseOrNull(linkText);
    if (reference == null) {
      return LinkResolution.error(null, "Not a Specman link: " + linkText);
    }
    File file = fileOf(reference);
    if (file == null) {
      return LinkResolution.error(null, "Unknown alias: " + reference.alias());
    }
    if (!file.isFile()) {
      return LinkResolution.error(file, "File not found");
    }
    if (!reference.hasStep()) {
      return LinkResolution.ok(file, linkTo(reference, file, null));
    }
    return resolveStep(reference, file);
  }

  private File fileOf(Reference reference) {
    return reference.hasAlias() ? LinkAliases.fileOf(reference.alias()) : reference.file();
  }

  private LinkResolution resolveStep(Reference reference, File file) {
    Map<String, String> stepNumbers;
    try {
      stepNumbers = stepNumbersOf(file);
    }
    catch (IOException | ModelParseException e) {
      return LinkResolution.error(file, "The file can't be read: " + e.getMessage());
    }
    String currentNumber = stepNumbers.get(reference.stepId());
    if (currentNumber == null) {
      return LinkResolution.error(file, "Step " + reference.stepId() + " doesn't exist in the file");
    }
    String link = linkTo(reference, file, currentNumber);
    return hasWrongNumber(reference, currentNumber) ? LinkResolution.sanitized(file, link) : LinkResolution.ok(file, link);
  }

  private boolean hasWrongNumber(Reference reference, String currentNumber) {
    return reference.stepNumber() != null && !reference.stepNumber().equals(currentNumber);
  }

  /** @return the link in the form the passed one has, with its alias if it has one, and the step number passed, which may be null */
  private String linkTo(Reference reference, File file, String stepNumber) {
    String alias = reference.hasAlias() ? reference.alias() : "";
    return SpecmanLink.of(file, alias, reference.stepId(), stepNumber);
  }

  /** @return the numbers the steps of the file have according to the file's structure, by step ID.
   * In general, both step IDs and step numbers are present in the file and could directly be
   * used as they are. However, we run a complete renumbering just in case the file is currently
   * under construction by a tool other than the Specman UI (especially by an AI agent) and wasn't
   * sanitized yet. This costs only a few milliseconds. */
  private Map<String, String> stepNumbersOf(File file) throws IOException, ModelParseException {
    String path = LinkAliases.absolutePath(file);
    Map<String, String> stepNumbers = stepNumbersByFile.get(path);
    if (stepNumbers == null) {
      DiagramModel_V002 model = ModelFileReader_V002.read(file);
      ModelRenumberer_V002.renumber(model.mainSequence);
      stepNumbers = ModelRenumberer_V002.collectNumbers(model.mainSequence);
      stepNumbersByFile.put(path, stepNumbers);
    }
    return stepNumbers;
  }
}
