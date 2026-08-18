package specman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Die ID eines Schrittes ist aus mehreren laufenden Nummern aufgebaut, die der üblichen Notation wie 1.19.7 entsprechen
 */
public class StepNumber implements Comparable<StepNumber> {
	public final List<Integer> numbers = new ArrayList<Integer>();

	public StepNumber() {} // For Jackson only

	public StepNumber(Integer... numbers) {
		this.numbers.addAll(Arrays.asList(numbers));
	}

	@Override
	public String toString() {
		if (numbers.isEmpty()) return "";
		StringBuffer b = new StringBuffer();
		for (Integer nummer: numbers) {
			b.append(nummer);
			b.append('.');
		}
		return b.substring(0, b.length() - 1);
	}

	public StepNumber naechsteID() {
		Integer[] naechsteNummern = numbers.toArray(new Integer[0]);
		naechsteNummern[naechsteNummern.length - 1]++;
		return new StepNumber(naechsteNummern);
	}

	public StepNumber vorhergehendeID() {
		Integer[] vorhergehendeNummern = numbers.toArray(new Integer[0]);
		vorhergehendeNummern[vorhergehendeNummern.length - 1]--;
		return new StepNumber(vorhergehendeNummern);
	}

	public StepNumber sameID() {
		return new StepNumber(numbers.toArray(new Integer[0]));
	}

	public StepNumber naechsteEbene() {
		Integer[] naechsteNummern = numbers.toArray(new Integer[numbers.size()+1]);
		naechsteNummern[naechsteNummern.length-1] = 0;
		return new StepNumber(naechsteNummern);
	}

	//Equals herstellen


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StepNumber stepNumber = (StepNumber) o;
		return Objects.equals(numbers, stepNumber.numbers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(numbers);
	}

  @Override
  public int compareTo(StepNumber other) {
    for (int i = 0; i < Math.min(numbers.size(), other.numbers.size()); i++) {
      int cmp = Integer.compare(numbers.get(i), other.numbers.get(i));
      if (cmp != 0) return cmp;
    }
    return Integer.compare(numbers.size(), other.numbers.size());
  }

  public static String asString(StepNumber id) {
    return id == null ? null : id.toString();
  }
}