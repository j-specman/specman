package classman.model.v001.change;

import classman.model.v001.Visibility_V001;

import java.util.List;

/** What a line of a method, as the user types it, says: the visibility, the name, the parameters, the return type, and the infos. */
public class MethodLine_V001 extends MemberLine_V001 {
    /** The texts of the parameters, each as shown: name: Type, or just Type. */
    public final List<String> parameters;

    public MethodLine_V001(Visibility_V001 visibility, String name, List<String> parameters, String returnType,
                           List<String> infos) {
        super(visibility, name, returnType, infos);
        this.parameters = List.copyOf(parameters);
    }
}
