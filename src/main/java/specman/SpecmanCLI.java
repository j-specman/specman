package specman;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import specman.model.ModelEnvelope;
import specman.model.v002.*;
import specman.model.v002.io.ModelParser_V002;
import specman.model.v002.io.ModelParseException;
import specman.model.v002.io.ModelRenumberer_V002;
import specman.model.v002.io.ModelSerializer_V002;
import specman.model.v002.io.ModelStepnumberRewriter_V002;
import specman.model.v002.io.PlainTextSynchronizer_V002;
import specman.model.v002.io.SanitizeResult;
import specman.model.v002.io.StepNumberChange;
import specman.model.v002.io.SteplinkUpdate;

import java.io.File;
import java.nio.charset.StandardCharsets;
import specman.ops.LoadDiagrammSpecmanOp;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static specman.ops.LoadDiagrammSpecmanOp.isTextFormat;

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
public class SpecmanCLI {

  public static void run(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: specman --sanitize <file.nsd>");
      System.exit(1);
    }
    File file = new File(args[1]);
    if (!file.exists()) {
      System.err.println("ERROR");
      System.err.println("File not found: " + file.getAbsolutePath());
      System.exit(1);
    }
    try {
      SanitizeResult result = sanitize(file);
      if (!result.brokenRefs.isEmpty()) {
        System.err.println("ERROR");
        System.err.println("Unresolvable steplink references: " + result.brokenRefs);
        System.exit(1);
      }
      if (result.hasChanges()) {
        System.out.println("SANITIZED");
        printChanges(result);
      }
      else {
        System.out.println("OK");
      }
      System.exit(0);
    }
    catch (ModelParseException e) {
      System.err.println("ERROR");
      System.err.println(e.getMessage());
      System.exit(1);
    }
    catch (Exception e) {
      System.err.println("ERROR");
      System.err.println(e.getMessage());
      System.exit(1);
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

  private static SanitizeResult sanitize(File file) throws Exception {
    byte[] data = Files.readAllBytes(file.toPath());

    DiagramModel_V002 model;
    if (isTextFormat(data)) {
      model = new ModelParser_V002().parse(new String(data, StandardCharsets.UTF_8));
    }
    else {
      ObjectMapper mapper = buildMapper();
      ModelEnvelope envelope = mapper.readValue(data, ModelEnvelope.class);
      if (!(envelope.model instanceof DiagramModel_V002)) {
        throw new Exception("Unsupported model type — headless sanitize requires V2 format");
      }
      model = (DiagramModel_V002) envelope.model;
    }

    List<String> idErrors = checkDuplicateIds(model);
    if (!idErrors.isEmpty()) {
      throw new Exception("Duplicate step IDs:\n  " + String.join("\n  ", idErrors));
    }

    Map<String, String> savedNumbers = ModelRenumberer_V002.collectNumbers(model.mainSequence);    ModelRenumberer_V002.renumber(model.mainSequence);
    Map<String, String> computedNumbers = ModelRenumberer_V002.collectNumbers(model.mainSequence);
    Map<String, String> numberMapping = ModelRenumberer_V002.buildNumberMapping(
        savedNumbers.isEmpty() ? computedNumbers : savedNumbers,
        computedNumbers);

    SanitizeResult result = ModelStepnumberRewriter_V002.rewrite(model, numberMapping);
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

  private static ObjectMapper buildMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper;
  }
}
