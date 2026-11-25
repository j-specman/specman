package classman.model.v001;

import java.util.Locale;

/** What a change set did to a diagram element. */
public enum ChangeType_V001 {
    ADDED,
    REMOVED;

    /** The word the text format uses: added or removed. */
    public String keyword() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return null if the word names no change type */
    public static ChangeType_V001 fromKeyword(String keyword) {
        for (ChangeType_V001 type : values()) {
            if (type.keyword().equals(keyword)) {
                return type;
            }
        }
        return null;
    }
}
