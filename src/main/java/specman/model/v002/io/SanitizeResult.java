package specman.model.v002.io;

import java.util.ArrayList;
import java.util.List;

public class SanitizeResult {
    public List<StepNumberChange> stepNumberChanges = new ArrayList<>();
    public final List<SteplinkUpdate> steplinkUpdates;
    public final List<String> brokenRefs;
    public int plainTextUpdates = 0;

    public SanitizeResult(List<SteplinkUpdate> steplinkUpdates, List<String> brokenRefs) {
        this.steplinkUpdates = steplinkUpdates;
        this.brokenRefs = brokenRefs;
    }

    public boolean hasChanges() {
        return !stepNumberChanges.isEmpty() || !steplinkUpdates.isEmpty() || plainTextUpdates > 0;
    }
}
