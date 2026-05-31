package specman.model.v001;

import specman.Aenderungsart;
import specman.ChangeInfo;

import java.util.List;

public class ListItemEditAreaModel_V001 extends AbstractEditAreaModel_V001 implements ChangeInfoBackwardsCompatible_V001 {
  public final EditorContentModel_V001 content;
  public final boolean ordered;
  public final Aenderungsart aenderungsart; // kept for backwards compatibility
  public final ChangeInfo_V001 changeInfo;

  public ListItemEditAreaModel_V001() {  // For Jackson only
    this.content = null;
    this.ordered = false;
    this.aenderungsart = null;
    this.changeInfo = null;
  }

  public ListItemEditAreaModel_V001(EditorContentModel_V001 content, boolean ordered, ChangeInfo changeInfo) {
    this.content = content;
    this.ordered = ordered;
    this.aenderungsart = asLegacyAenderungsart(changeInfo);
    this.changeInfo = asChangeInfo(changeInfo);
  }
}
