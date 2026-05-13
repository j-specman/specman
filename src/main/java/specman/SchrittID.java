package specman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Die ID eines Schrittes ist aus mehreren laufenden Nummern aufgebaut, die der üblichen Notation wie 1.19.7 entsprechen
 */
public class SchrittID implements Comparable<SchrittID> {
	public final List<Integer> nummern = new ArrayList<Integer>();

	public SchrittID() {} // For Jackson only

	public SchrittID(Integer... nummern) {
		this.nummern.addAll(Arrays.asList(nummern));
	}

	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		for (Integer nummer: nummern) {
			b.append(nummer);
			b.append('.');
		}
		return b.substring(0, b.length() - 1);
	}

	public SchrittID naechsteID() {
		Integer[] naechsteNummern = nummern.toArray(new Integer[0]);
		naechsteNummern[naechsteNummern.length - 1]++;
		return new SchrittID(naechsteNummern);
	}

	public SchrittID vorhergehendeID() {
		Integer[] vorhergehendeNummern = nummern.toArray(new Integer[0]);
		vorhergehendeNummern[vorhergehendeNummern.length - 1]--;
		return new SchrittID(vorhergehendeNummern);
	}

	public SchrittID sameID() {
		return new SchrittID(nummern.toArray(new Integer[0]));
	}

	public SchrittID naechsteEbene() {
		Integer[] naechsteNummern = nummern.toArray(new Integer[nummern.size()+1]);
		naechsteNummern[naechsteNummern.length-1] = 0;
		return new SchrittID(naechsteNummern);
	}

	//Equals herstellen


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SchrittID schrittID = (SchrittID) o;
		return Objects.equals(nummern, schrittID.nummern);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nummern);
	}

  @Override
  public int compareTo(SchrittID other) {
    for (int i = 0; i < Math.min(nummern.size(), other.nummern.size()); i++) {
      int cmp = Integer.compare(nummern.get(i), other.nummern.get(i));
      if (cmp != 0) return cmp;
    }
    return Integer.compare(nummern.size(), other.nummern.size());
  }

  public static String asString(SchrittID id) {
    return id == null ? null : id.toString();
  }
}