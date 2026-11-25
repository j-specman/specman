package specman.cli;

import specman.cli.sanitize.ChangeTrackingValidator;
import specman.cli.sanitize.DuplicateIdValidator;
import specman.cli.sanitize.MetaTagValidator;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.io.ModelFileReader_V002;
import specman.model.v002.io.ModelParseException;
import specman.model.v002.io.ModelRenumberer_V002;
import specman.model.v002.io.ParseOutcome;
import specman.model.v002.io.ModelSerializer_V002;
import specman.model.v002.io.ModelStepnumberRewriter_V002;
import specman.model.v002.io.PlainTextSynchronizer_V002;
import specman.model.v002.io.SanitizeResult;
import specman.model.v002.io.StepNumberChange;
import specman.model.v002.io.SteplinkUpdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

  private static final int REQUIRED_ARGUMENT_COUNT = 2;
  private static final int FILENAME_ARGUMENT_INDEX = 1;

  private final String filename;

  SanitizeCLO(String[] args) throws CLIException {
    assertFileArgumentGiven(args);
    this.filename = args[FILENAME_ARGUMENT_INDEX];
  }

  private static void assertFileArgumentGiven(String[] args) throws CLIException {
    assertThat(args.length >= REQUIRED_ARGUMENT_COUNT, "Usage: specman " + CLIOperation.SANITIZE + " <file>");
  }

  @Override
  int run() throws CLIException {
    File file = new File(filename);
    assertFileExists(file);
    printOutcome(sanitize(file));
    return EXIT_SUCCESS;
  }

  private static SanitizeResult sanitize(File file) throws CLIException {
    ParseOutcome outcome = readModel(file);
    DiagramModel_V002 model = outcome.model();
    SanitizeModelValidator.assertNoDuplicateIds(model);
    SanitizeModelValidator.assertValidMetaTags(model);

    Renumbering renumbering = renumberSteps(model);
    SanitizeResult result = ModelStepnumberRewriter_V002.rewrite(model, renumbering.toNumberMapping());
    assertNoBrokenRefs(result);

    result.stepNumberChanges = ModelRenumberer_V002.buildStepNumberChanges(
      model.mainSequence, renumbering.savedNumbers, renumbering.computedNumbers);
    result.plainTextUpdates = PlainTextSynchronizer_V002.updateAllPlainTexts(model);
    SanitizeChangeTrackingValidator.assertValidChangeTracking(outcome);

    writeIfChanged(file, model, result);
    return result;
  }

  private static ParseOutcome readModel(File file) throws CLIException {
    try {
      return ModelFileReader_V002.readWithDiagnostics(file);
    }
    catch (IOException | ModelParseException e) {
      throw error(e.getMessage());
    }
  }

  private static Renumbering renumberSteps(DiagramModel_V002 model) {
    Map<String, String> savedNumbers = ModelRenumberer_V002.collectNumbers(model.mainSequence);
    ModelRenumberer_V002.renumber(model.mainSequence);
    Map<String, String> computedNumbers = ModelRenumberer_V002.collectNumbers(model.mainSequence);
    return new Renumbering(savedNumbers, computedNumbers);
  }

  private static void assertNoBrokenRefs(SanitizeResult result) throws CLIException {
    assertThat(result.brokenRefs.isEmpty(), "Unresolvable steplink references: " + result.brokenRefs);
  }

  private static void writeIfChanged(File file, DiagramModel_V002 model, SanitizeResult result) throws CLIException {
    if (result.hasChanges()) {
      write(file, new ModelSerializer_V002().serialize(model));
    }
  }

  private static void write(File file, String content) throws CLIException {
    try {
      Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException e) {
      throw error("File can't be written: " + file.getAbsolutePath() + ": " + e.getMessage());
    }
  }

  private void printOutcome(SanitizeResult result) {
    if (!result.hasChanges()) {
      out.println("OK");
      return;
    }
    out.println("SANITIZED");
    printStepNumberChanges(result.stepNumberChanges);
    printSteplinkUpdates(result.steplinkUpdates);
    printPlainTextUpdates(result.plainTextUpdates);
  }

  private void printStepNumberChanges(List<StepNumberChange> changes) {
    if (changes.isEmpty()) {
      return;
    }
    out.println();
    out.println("Step numbers changed (" + changes.size() + "):");
    for (StepNumberChange change : changes) {
      out.println("  " + describe(change));
    }
  }

  private static String describe(StepNumberChange change) {
    String text = change.plainText.isEmpty() ? "" : " \"" + change.plainText + "\"";
    return describeNumberChange(change.oldNumber, change.newNumber) + " (" + change.stepType + ")" + text;
  }

  private void printSteplinkUpdates(List<SteplinkUpdate> updates) {
    if (updates.isEmpty()) {
      return;
    }
    out.println();
    out.println("Steplink references updated (" + updates.size() + "):");
    for (Map.Entry<String, List<String>> entry : groupByLocation(updates).entrySet()) {
      out.println("  " + entry.getKey() + ": " + String.join(", ", entry.getValue()));
    }
  }

  private static Map<String, List<String>> groupByLocation(List<SteplinkUpdate> updates) {
    Map<String, List<String>> changesByLocation = new LinkedHashMap<>();
    for (SteplinkUpdate update : updates) {
      changesByLocation.computeIfAbsent(update.location, location -> new ArrayList<>())
        .add(describeNumberChange(update.oldNumber, update.newNumber));
    }
    return changesByLocation;
  }

  private static String describeNumberChange(String oldNumber, String newNumber) {
    return oldNumber + " -> " + newNumber;
  }

  private void printPlainTextUpdates(int plainTextUpdates) {
    if (plainTextUpdates > 0) {
      out.println();
      out.println("Plain text fields refreshed: " + plainTextUpdates);
    }
  }

  /** The step numbers before and after the steps have been renumbered. */
  private record Renumbering(Map<String, String> savedNumbers, Map<String, String> computedNumbers) {

    /** Maps the old to the new numbers. A model without saved numbers has nothing to map. */
    Map<String, String> toNumberMapping() {
      return ModelRenumberer_V002.buildNumberMapping(savedNumbers.isEmpty() ? computedNumbers : savedNumbers, computedNumbers);
    }
  }
}
