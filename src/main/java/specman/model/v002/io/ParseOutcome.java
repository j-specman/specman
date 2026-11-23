package specman.model.v002.io;

import specman.model.v002.DiagramModel_V002;

import java.util.List;

/** The result of parsing a model file together with what is known about it beyond the model itself.
 * @param model the parsed model
 * @param positions where the elements of the model have been written in the file
 * @param diagnostics problems which didn't prevent building the model but which the model no longer shows, each one
 *                    starting with the position it was found at, e.g. "line 14:62 ..." */
public record ParseOutcome(DiagramModel_V002 model, ModelSourcePositions positions, List<String> diagnostics) {

  /** The outcome of reading a file which has no positions, as it is not in text format. */
  static ParseOutcome withoutPositions(DiagramModel_V002 model) {
    return new ParseOutcome(model, new ModelSourcePositions(), List.of());
  }
}
