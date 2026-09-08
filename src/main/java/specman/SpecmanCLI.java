package specman;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import specman.model.ModelEnvelope;
import specman.model.v002.*;
import specman.model.v002.io.ModelRenumberer_V002;
import specman.model.v002.io.ModelStepnumberRewriter_V002;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Headless validator for model files written by AI agents.
 * Validates the model, corrects stale step numbers and steplink texts,
 * and writes the corrected model back to disk.
 *
 * Exit code 0 = valid and corrected.
 * Exit code 1 = invalid (parse error or broken steplink references).
 */
public class SpecmanCLI {

  public static void run(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: specman --validate <file.nsd.wrk>");
      System.exit(1);
    }
    File file = new File(args[1]);
    if (!file.exists()) {
      System.err.println("File not found: " + file.getAbsolutePath());
      System.exit(1);
    }
    long start = System.currentTimeMillis();
    try {
      List<String> brokenRefs = validateAndFix(file);
      long ms = System.currentTimeMillis() - start;
      if (brokenRefs.isEmpty()) {
        System.out.println("OK (" + ms + " ms)");
        System.exit(0);
      }
      else {
        System.err.println("INVALID (" + ms + " ms): unresolvable steplink references: " + brokenRefs);
        System.exit(1);
      }
    }
    catch (Exception e) {
      long ms = System.currentTimeMillis() - start;
      System.err.println("INVALID (" + ms + " ms): " + e.getMessage());
      System.exit(1);
    }
  }

  private static List<String> validateAndFix(File file) throws Exception {
    ObjectMapper mapper = buildMapper();
    ModelEnvelope envelope = mapper.readValue(file, ModelEnvelope.class);
    if (!(envelope.model instanceof DiagramModel_V002 model)) {
      throw new Exception("Unsupported model type: " + envelope.modelType +
          " — headless validation requires V2 format");
    }

    Map<String, String> savedNumbers = ModelRenumberer_V002.collectNumbers(model.mainSequence);
    ModelRenumberer_V002.renumber(model.mainSequence);
    Map<String, String> computedNumbers = ModelRenumberer_V002.collectNumbers(model.mainSequence);
    Map<String, String> numberMapping = ModelRenumberer_V002.buildNumberMapping(
        savedNumbers.isEmpty() ? computedNumbers : savedNumbers,
        computedNumbers);

    List<String> brokenRefs = ModelStepnumberRewriter_V002.rewrite(model, numberMapping);
    if (!brokenRefs.isEmpty()) {
      return brokenRefs;
    }

    DiagramModel_V002 corrected = new DiagramModel_V002(
        model.name, model.width, model.zoomFactor, model.changeModeEnabled,
        model.mainSequence, model.intro, model.outro,
        model.pdfExportOptions, model.changeSetName);

    ModelEnvelope correctedEnvelope = new ModelEnvelope();
    correctedEnvelope.model = corrected;
    correctedEnvelope.modelType = DiagramModel_V002.class.getName();
    correctedEnvelope.specmanVersion = SpecmanVersion.getVersion();

    mapper.writerWithDefaultPrettyPrinter().writeValue(file, correctedEnvelope);
    return brokenRefs;
  }

  private static ObjectMapper buildMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper;
  }
}
