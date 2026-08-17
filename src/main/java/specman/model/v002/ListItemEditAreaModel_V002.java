package specman.model.v002;

import specman.ChangeInfo;

public class ListItemEditAreaModel_V002 extends AbstractEditAreaModel_V002 {
    public final EditorContentModel_V002 content;
    public final boolean ordered;
    public final ChangeInfoModel_V002 changeInfo;

    @Deprecated public ListItemEditAreaModel_V002() { // For Jackson only
        content = null;
        ordered = false;
        changeInfo = null;
    }

    public ListItemEditAreaModel_V002(EditorContentModel_V002 content, boolean ordered, ChangeInfo changeInfo) {
        this.content = content;
        this.ordered = ordered;
        this.changeInfo = ChangeInfoModel_V002.from(changeInfo);
    }
}
