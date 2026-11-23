package specman.cli;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum CLIOperation {
    SANITIZE("--sanitize"),
    RESOLVE_LINK("--resolve-link"),
    VERSION("--version");

    private final String operationName;

    CLIOperation(String operationName) {
        this.operationName = operationName;
    }

    public static CLIOperation fromArgs(String[] args) {
      if (args.length > 0) {
        for (CLIOperation op : CLIOperation.values()) {
          if (op.getOperationName().equalsIgnoreCase(args[0])) {
            return op;
          }
        }
      }
      return null;
    }

    /** @return the names of all operations, separated by commas, for messages */
    public static String validOptions() {
      return Arrays.stream(values()).map(CLIOperation::getOperationName).collect(Collectors.joining(", "));
    }

    public String getOperationName() {
      return operationName;
    }

    public String toString() {
      return operationName;
    }
}
