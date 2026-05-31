package specman.model.v001;

import specman.Aenderungsart;
import specman.ChangeInfo;

/**
 * Mixin for model classes that carry change-tracking state. Ensures that both the modern
 * {@link ChangeInfo_V001} field and the legacy {@code aenderungsart} field are always written
 * consistently when saving a file. The legacy field exists solely so that older Specman versions
 * (1.1.x), which only read {@code aenderungsart}, can still open files saved by newer versions.
 * <p>
 * Model constructors should use {@link #asLegacyAenderungsart} and {@link #asChangeInfo} to
 * populate both fields from a single runtime {@link specman.ChangeInfo} argument, keeping the
 * conversion logic in one place.
 */
public interface ChangeInfoBackwardsCompatible_V001 {
  default Aenderungsart asLegacyAenderungsart(ChangeInfo changeInfo) {
    return changeInfo != null ? changeInfo.art() : null;
  }
  default ChangeInfo_V001 asChangeInfo(ChangeInfo changeInfo) {
    return changeInfo != null ? new ChangeInfo_V001(changeInfo) : null;
  }

}
