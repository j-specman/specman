package specman.model.v001;

import specman.Aenderungsart;
import specman.ChangeInfo;

import java.util.List;

public class TableEditAreaModel_V001 extends AbstractEditAreaModel_V001 implements ChangeInfoBackwardsCompatible_V001 {
  public final List<List<EditorContentModel_V001>> cells;
  public final int tableWidthPercent;
  public final List<Integer> columnsWidthPercent;
  public final Aenderungsart aenderungsart; // kept for backwards compatibility
  public final ChangeInfo_V001 changeInfo;

  public TableEditAreaModel_V001() {  // For Jackson only
    this.cells = null;
    this.tableWidthPercent = 0;
    this.columnsWidthPercent = null;
    this.aenderungsart = null;
    this.changeInfo = null;
  }

  public TableEditAreaModel_V001(List<List<EditorContentModel_V001>> cells, int tableWidthPercent, List<Integer> columnsWidthPercent, ChangeInfo changeInfo) {
    this.cells = cells;
    this.tableWidthPercent = tableWidthPercent;
    this.columnsWidthPercent = columnsWidthPercent;
    this.aenderungsart = asLegacyAenderungsart(changeInfo);
    this.changeInfo = asChangeInfo(changeInfo);
  }
}
