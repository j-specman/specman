package specman.model.v002;

import specman.ChangeInfo;

import java.util.List;

public class TableEditAreaModel_V002 extends AbstractEditAreaModel_V002 {
    public final List<List<EditorContentModel_V002>> cells;
    public final int tableWidthPercent;
    public final List<Integer> columnsWidthPercent;
    public final ChangeInfoModel_V002 changeInfo;

    @Deprecated public TableEditAreaModel_V002() { // For Jackson only
        cells = null;
        tableWidthPercent = 0;
        columnsWidthPercent = null;
        changeInfo = null;
    }

    public TableEditAreaModel_V002(List<List<EditorContentModel_V002>> cells, int tableWidthPercent, List<Integer> columnsWidthPercent, ChangeInfo changeInfo) {
        this.cells = cells;
        this.tableWidthPercent = tableWidthPercent;
        this.columnsWidthPercent = columnsWidthPercent;
        this.changeInfo = ChangeInfoModel_V002.from(changeInfo);
    }
}
