package specman.model.v001;

import com.fasterxml.jackson.annotation.JsonIgnore;
import specman.ChangeInfo;

import java.util.ArrayList;
import java.util.List;

public class EditorContentModel_V001 {
  public final List<AbstractEditAreaModel_V001> areas;

  public EditorContentModel_V001() {
    areas = new ArrayList<>();
  }

  public EditorContentModel_V001(String initialContent, ChangeInfo changeInfo) {
    this();
    addArea(new TextEditAreaModel_V001(initialContent, initialContent, new ArrayList<>(), changeInfo));
  }

  public static EditorContentModel_V001 empty() {
    return new EditorContentModel_V001(new TextEditAreaModel_V001(""));
  }

  public EditorContentModel_V001(AbstractEditAreaModel_V001 initialContent) {
    this();
    addArea(initialContent);
  }

  public EditorContentModel_V001(List<AbstractEditAreaModel_V001> areas) {
    this.areas = areas;
  }

  @Deprecated
  @JsonIgnore
  public TextEditAreaModel_V001 getFirstAreaAsText() {
    return (TextEditAreaModel_V001) areas.get(0);
  }

  public void addArea(AbstractEditAreaModel_V001 area) {
    areas.add(area);
  }
}
