package specman.ops;

import specman.EditException;
import specman.editarea.TableEditArea;

public class CutTableOp extends AbstractADBLSpecmanOp {

  private final TableEditArea table;

  public CutTableOp(SpecmanOpContext context, TableEditArea table) {
    super(context);
    this.table = table;
  }

  @Override
  void execute() throws EditException {
    new CopyTableOp(table).run();
    table.removeTableOrMarkAsDeletedUDBL();
  }
}
