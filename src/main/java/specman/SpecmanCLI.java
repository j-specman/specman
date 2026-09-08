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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Headless validator for model files written by AI agents.
 * Validates the model, corrects stale step numbers and steplink texts,
 * and writes the corrected model back to disk in V2 text format.
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

  private static boolean isTextFormat(byte[] data) {
    return data.length >= 2 && data[0] == '/' && data[1] == '/';
  }

  private static List<String> validateAndFix(File file) throws Exception {
    byte[] data = Files.readAllBytes(file.toPath());

    DiagramModel_V002 model;
    if (isTextFormat(data)) {
      try {
        model = new ModelParser_V002().parse(new String(data, StandardCharsets.UTF_8));
      }
      catch (ModelParseException e) {
        throw new Exception("Parse error: " + e.getMessage(), e);
      }
    }
    else {
      ObjectMapper mapper = buildMapper();
      ModelEnvelope envelope = mapper.readValue(data, ModelEnvelope.class);
      if (!(envelope.model instanceof DiagramModel_V002)) {
        throw new Exception("Unsupported model type: " + envelope.modelType +
            " — headless validation requires V2 format");
      }
      model = (DiagramModel_V002) envelope.model;
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

    String corrected = new ModelSerializer_V002().serialize(model);
    Files.write(file.toPath(), corrected.getBytes(StandardCharsets.UTF_8));
    return brokenRefs;
  }

  private static ObjectMapper buildMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper;
  }
}
