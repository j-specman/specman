package specman.model.v002.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import specman.model.ModelEnvelope;
import specman.model.v002.DiagramModel_V002;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static specman.ops.LoadDiagrammSpecmanOp.isTextFormat;

/** Reads a diagram file into its V2 model without Swing, for the command line tools. Files in an older format are not
 * converted, as the tools only look at files of the current format. */
public class ModelFileReader_V002 {

  /** @throws IOException if the file can't be read or doesn't hold a V2 model
   * @throws ModelParseException if the text format of the file is broken */
  public static DiagramModel_V002 read(File file) throws IOException, ModelParseException {
    byte[] data = Files.readAllBytes(file.toPath());
    if (isTextFormat(data)) {
      return new ModelParser_V002().parse(new String(data, StandardCharsets.UTF_8));
    }
    ModelEnvelope envelope = buildMapper().readValue(data, ModelEnvelope.class);
    if (!(envelope.model instanceof DiagramModel_V002)) {
      throw new IOException("Unsupported model type — only the V2 format can be read headlessly");
    }
    return (DiagramModel_V002) envelope.model;
  }

  private static ObjectMapper buildMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper;
  }
}
