package classman.model.v001.change;

import classman.model.v001.Visibility_V001;

import java.util.List;

/** What a line of an attribute, as the user types it, says: the visibility, the name, the type, and the infos. */
public class AttributeLine_V001 extends MemberLine_V001 {

    public AttributeLine_V001(Visibility_V001 visibility, String name, String type, List<String> infos) {
        super(visibility, name, type, infos);
    }
}
