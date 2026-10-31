package specman.cli;

import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.MetaTagConfigModel_V002;
import specman.model.v002.MetaTagModel_V002;
import specman.model.v002.io.ModelFileReader_V002;
import specman.model.v002.io.ModelParseException;
import specman.model.v002.io.ModelRenumberer_V002;
import specman.model.v002.io.ModelSerializer_V002;
import specman.model.v002.io.ModelStepnumberRewriter_V002;
import specman.model.v002.io.PlainTextSynchronizer_V002;
import specman.model.v002.io.SanitizeResult;
import specman.model.v002.io.StepNumberChange;
import specman.model.v002.io.SteplinkUpdate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static specman.cli.SpecmanCLI.EXIT_SUCCESS;

/**
 * Headless sanitizer for model files written by AI agents.
 * Corrects stale step numbers and steplink texts, and writes the
 * corrected model back to disk in V2 text format.
 *
 * Output (first line always one of):
 *   OK        — file valid, no changes needed
 *   SANITIZED — file valid, changes applied (details follow)
 *   ERROR     — parse or consistency error (details follow on stderr)
 *
 * Exit code 0 = OK or SANITIZED.
 * Exit code 1 = ERROR.
 */
public class SanitizeCLO extends AbstractCLO {

  private final String filename;

  SanitizeCLO(String[] args) throws CLIException {
    if (args.length < 2) {
      throw error("Usage: specman " + CLIOperation.SANITIZE + " <file>");
    }
    this.filename = args[1];
  }

  int run() {
    File file = new File(filename);
    if (!file.exists()) {
      throw error("File not found: " + file.getAbsolutePath());
    }
    try {
      SanitizeResult result = sanitize(file);
      if (!result.brokenRefs.isEmpty()) {
        throw error("Unresolvable steplink references: " + result.brokenRefs);
      }
      if (result.hasChanges()) {
        System.out.println("SANITIZED");
        printChanges(result);
      }
      else {
        System.out.println("OK");
      }
      return EXIT_SUCCESS;
    }
    catch (Exception e) {
      throw error(e.getMessage());
    }
  }

  private static void printChanges(SanitizeResult result) {
    if (!result.stepNumberChanges.isEmpty()) {
      System.out.println();
      System.out.println("Step numbers changed (" + result.stepNumberChanges.size() + "):");
      for (StepNumberChange c : result.stepNumberChanges) {
        String text = c.plainText.isEmpty() ? "" : " \"" + c.plainText + "\"";
        System.out.println("  " + c.oldNumber + " -> " + c.newNumber + " (" + c.stepType + ")" + text);
      }
    }
    if (!result.steplinkUpdates.isEmpty()) {
      // Group by location for compact output
      java.util.LinkedHashMap<String, java.util.List<String>> byLocation = new java.util.LinkedHashMap<>();
      for (SteplinkUpdate u : result.steplinkUpdates) {
        byLocation.computeIfAbsent(u.location, k -> new java.util.ArrayList<>())
          .add(u.oldNumber + " -> " + u.newNumber);
      }
      System.out.println();
      System.out.println("Steplink references updated (" + result.steplinkUpdates.size() + "):");
      for (Map.Entry<String, java.util.List<String>> entry : byLocation.entrySet()) {
        System.out.println("  " + entry.getKey() + ": " + String.join(", ", entry.getValue()));
      }
    }
    if (result.plainTextUpdates > 0) {
      System.out.println();
      System.out.println("Plain text fields refreshed: " + result.plainTextUpdates);
    }
  }

  private static SanitizeResult sanitize(File file) throws IOException, ModelParseException {
    DiagramModel_V002 model = ModelFileReader_V002.read(file);

    List<String> idErrors = checkDuplicateIds(model);
    if (!idErrors.isEmpty()) {
      throw new RuntimeException("Duplicate step IDs:\n  " + String.join("\n  ", idErrors));
    }

    List<String> tagErrors = checkMetaTags(model);
    if (!tagErrors.isEmpty()) {
      throw new RuntimeException("Invalid meta tags:\n  " + String.join("\n  ", tagErrors));
    }

    Map<String, String> savedNumbers = ModelRenumberer_V002.collectNumbers(model.mainSequence);
    ModelRenumberer_V002.renumber(model.mainSequence);
    Map<String, String> computedNumbers = ModelRenumberer_V002.collectNumbers(model.mainSequence);
    Map<String, String> numberMapping = ModelRenumberer_V002.buildNumberMapping(
      savedNumbers.isEmpty() ? computedNumbers : savedNumbers,
      computedNumbers);

    SanitizeResult result;
    try {
      result = ModelStepnumberRewriter_V002.rewrite(model, numberMapping);
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    if (!result.brokenRefs.isEmpty()) {
      return result;
    }

    result.stepNumberChanges = ModelRenumberer_V002.buildStepNumberChanges(
      model.mainSequence, savedNumbers, computedNumbers);
    result.plainTextUpdates = PlainTextSynchronizer_V002.updateAllPlainTexts(model);

    if (result.hasChanges()) {
      String corrected = new ModelSerializer_V002().serialize(model);
      Files.write(file.toPath(), corrected.getBytes(StandardCharsets.UTF_8));
    }
    return result;
  }

  private static List<String> checkDuplicateIds(DiagramModel_V002 model) {
    Map<String, List<String>> idToStepNumbers = new LinkedHashMap<>();
    for (AbstractStepModel_V002 step : model.queryAllSteps()) {
      idToStepNumbers.computeIfAbsent(step.id, k -> new ArrayList<>())
        .add(step.stepNumber != null ? step.stepNumber : "?");
    }
    List<String> errors = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : idToStepNumbers.entrySet()) {
      if (entry.getValue().size() > 1) {
        errors.add("ID " + entry.getKey() + " used by steps: " + entry.getValue());
      }
    }
    return errors;
  }

  private static List<String> checkMetaTags(DiagramModel_V002 model) {
    Map<String, MetaTagConfigModel_V002> configs = new LinkedHashMap<>();
    List<String> errors = new ArrayList<>();
    for (MetaTagConfigModel_V002 config : model.metaTagConfigs) {
      if (configs.put(config.name, config) != null) {
        errors.add("Meta tag configuration '" + config.name + "' is defined more than once");
      }
      boolean hasLabel = config.label != null && !config.label.isEmpty();
      boolean hasIcon = config.iconSvg != null && !config.iconSvg.isEmpty();
      if (!hasLabel && !hasIcon) {
        errors.add("Meta tag configuration '" + config.name + "' has neither label nor icon");
      }
    }
    for (AbstractStepModel_V002 step : model.queryAllSteps()) {
      String stepLabel = "Step " + (step.stepNumber != null ? step.stepNumber : "?") + " (" + step.id + ")";
      Set<String> assignedConfigNames = new HashSet<>();
      for (MetaTagModel_V002 tag : step.tags) {
        if (!assignedConfigNames.add(tag.configName)) {
          errors.add(stepLabel + ": tag '" + tag.configName + "' is assigned more than once");
        }
        MetaTagConfigModel_V002 config = configs.get(tag.configName);
        if (config == null) {
          errors.add(stepLabel + ": tag '" + tag.configName + "' has no configuration");
        }
        else if (!config.freetextAllowed && tag.freetext != null && !tag.freetext.isEmpty()) {
          errors.add(stepLabel + ": tag '" + tag.configName + "' has freetext but its configuration does not allow freetext");
        }
      }
    }
    return errors;
  }

}
