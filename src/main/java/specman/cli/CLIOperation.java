package specman.cli;

public enum CLIOperation {
    SANITIZE("--sanitize");

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

    public String getOperationName() {
      return operationName;
    }

    public String toString() {
      return operationName;
    }
}
