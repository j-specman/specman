package specman.metatag;

import specman.ChangeSet;

public interface KlappbarerBereichI {

	void geklappt(boolean auf);

	/** @return the changeset of the first change mark found in this area, or null if there is none */
	ChangeSet firstChangeSet();

	default boolean enthaeltAenderungsmarkierungen() {
		return firstChangeSet() != null;
	}

}
