package specman.model.v002;

import com.fasterxml.jackson.annotation.JsonIgnore;
import specman.ChangeInfo;

import java.util.ArrayList;
import java.util.List;

public class EditorContentModel_V002 {
    public final List<AbstractEditAreaModel_V002> areas;

    public EditorContentModel_V002() {
        areas = new ArrayList<>();
    }

    public EditorContentModel_V002(AbstractEditAreaModel_V002 initialContent) {
        this();
        areas.add(initialContent);
    }

    public EditorContentModel_V002(List<AbstractEditAreaModel_V002> areas) {
        this.areas = areas;
    }

    public EditorContentModel_V002(String initialContent, ChangeInfo changeInfo) {
        this();
        areas.add(new TextEditAreaModel_V002(initialContent, initialContent, new ArrayList<>(), changeInfo));
    }

    public static EditorContentModel_V002 empty() {
        return new EditorContentModel_V002(new TextEditAreaModel_V002(""));
    }

    @Deprecated
    @JsonIgnore
    public TextEditAreaModel_V002 getFirstAreaAsText() {
        return (TextEditAreaModel_V002) areas.get(0);
    }

    public void addArea(AbstractEditAreaModel_V002 area) {
        areas.add(area);
    }
}
