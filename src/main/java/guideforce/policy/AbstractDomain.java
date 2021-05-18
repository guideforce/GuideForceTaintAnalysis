package guideforce.policy;


import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstraction of Büchi automata.
 * <p>
 * It gives access to to the following structure:
 * <p><ul>
 * <li> Syntactic monoid
 * <li> Finitary abstraction to abstract languages of finite words.
 * <li> Infinitary abstraction to abstract languages of finite and infinite words.
 * </ul></p>
 *
 * <p>
 * The elements of the syntactic monoid are abstract and are just numbered
 * 0, 1, ..., n. Integers are used to represent them.
 * </p>
 *
 * <p>
 * Finite and infinite abstractions are sets of monoid elements and of
 * tuples of such elements respectively.
 * These sets are represented by the <b>immutable</b> inner classes
 * {@code Finitary} and {@code Infinitary}.
 * Invariants, such as that intraprocedural abstraction must be
 * <b>closed</b> sets of tuples are enforced.
 * </p>
 */
public abstract class AbstractDomain {

  //region Monoid operations
  //---------------------------------------------------------------------------

  private final Finitary BOTTOM_FINITARY = new Finitary();

  /**
   * Returns the neutral element of the monoid.
   */
  public abstract int neutral();

  /**
   * Multiplies of monoid elements.
   */
  public abstract int multiply(int x, int y);

  /**
   * Returns the element of the syntactic monoid for an alphabet symbol.
   *
   * @param token alphabet symbol
   * @return syntactic class of alphabet symbol
   */
  public abstract int read(Object token);

  /**
   * Converts a monoid element into a string.
   */
  public abstract String monoidElementToString(int x);

  public abstract Set<Integer> getAllMonoidElements();

  //---------------------------------------------------------------------------
  //endregion

  //region Finitary operations
  //---------------------------------------------------------------------------

  /**
   * Returns the element of the syntactic monoid for a word.
   *
   * @param tokens word, represented as a list of alphabet symbols
   * @return syntactic class of word
   */
  public int read(List<Object> tokens) {
    Objects.requireNonNull(tokens);
    int result = neutral();
    for (Object token : tokens) {
      result = multiply(result, read(token));
    }
    return result;
  }

  /**
   * Bottom element of the finitary abstraction, i.e. the empty set.
   */
  public Finitary zeroFinitary() {
    return BOTTOM_FINITARY;
  }

  /**
   * Multiplicative unit for the finitary abstraction, i.e. the singleton of
   * the monoid unit.
   */
  public Finitary oneFinitary() {
    return new Finitary(Collections.singleton(neutral()));
  }

  /**
   * Finite abstraction from monoid elements.
   */
  public Finitary makeFinitary(int... xs) {
    Set<Integer> elements = new TreeSet<>();
    for (int x : xs) {
      elements.add(x);
    }
    return new Finitary(elements);
  }

  /**
   * Multiplication of finitary abstractions by pointwise monoid multiplication.
   */
  public Finitary multiply(Finitary x, Finitary y) {
    Objects.requireNonNull(x);
    Objects.requireNonNull(y);
    Set<Integer> output = new TreeSet<>();
    for (Integer clsA : x.classes) {
      for (Integer clsB : y.classes) {
        output.add(AbstractDomain.this.multiply(clsA, clsB));
      }
    }
    return new Finitary(output);
  }

  /**
   * Kleene star on finitary abstractions.
   *
   * @param x abstraction of finitary language
   * @return abstraction of L^*, where L is the language represented by {@code x}
   */
  public Finitary star(Finitary x) {
    Objects.requireNonNull(x);
    return new Finitary(star(x.classes));
  }

  /**
   * Omega language for finitary abstractions.
   *
   * @param x abstraction of finitary language
   * @return abstraction of L^omega, where L is the language represented by {@code x}
   */
  public Infinitary omega(Finitary x) {
    Objects.requireNonNull(x);
    return new Infinitary(omega(x.classes));
  }

  //---------------------------------------------------------------------------
  //endregion

  //region Infinitary operations
  //---------------------------------------------------------------------------

  /**
   * Returns if the abstracted language is accepted.
   *
   * @param x abstraction of finitary language
   * @return abstraction of L^omega, where L is the language represented by {@code x}
   */
  public boolean acceptedFinitary(Finitary x) {
    Objects.requireNonNull(x);
    return acceptedFinitary(x.classes);
  }

  public Finitary getAcceptedFinitary(){
    return new Finitary(getAcceptedFinitaryClasses());
  }

  public Infinitary getAcceptedInfinitary() {
    return new Infinitary(getAcceptedInfinitaryClasses());
  }

  /**
   * Bottom element of the intraprocedural abstraction, i.e. the empty set.
   */
  public Infinitary zeroInfinitary() {
    return new Infinitary();
  }

  /**
   * Multiplication of a finite abstraction with an infinite abstraction.
   */
  public Infinitary multiply(Finitary x, Infinitary y) {
    return new Infinitary(concat(x.classes, y.tuples));
  }

  //---------------------------------------------------------------------------
  //endregion

  public boolean acceptedInfinitary(Infinitary x) {
    return acceptedInfinitary(x.tuples);
  }

  //region Abstract methods that need to be implemented
  //---------------------------------------------------------------------------

  protected abstract Set<Integer> star(Set<Integer> x);

  protected abstract Set<Tuple> omega(Set<Integer> x);

  protected abstract Set<Tuple> concat(Set<Integer> x, Set<Tuple> y);

  protected abstract boolean acceptedFinitary(Set<Integer> x);

  protected abstract boolean acceptedInfinitary(Set<Tuple> x);

  protected abstract Set<Integer> getAcceptedFinitaryClasses();

  protected abstract Set<Tuple> getAcceptedInfinitaryClasses();

  //---------------------------------------------------------------------------
  //endregion

  /**
   * Represents an abstraction of a language of finite words.
   */
  @Immutable
  public final class Finitary {

    // BitSets would be more efficient
    private final Set<Integer> classes;

    private Finitary() {
      classes = new TreeSet<>();
    }

    private Finitary(Set<Integer> classes) {
      this.classes = classes;
    }

    /**
     * Returns an abstraction of the union of this language with another language x.
     */
    public Finitary join(Finitary x) {
      Set<Integer> classes = new TreeSet<>(this.classes);
      classes.addAll(x.classes);
      return new Finitary(classes);
    }

    /**
     * Returns an abstraction of the union of this language with the singleton
     * abstraction {x}.
     */
    public Finitary join(Integer x) {
      Set<Integer> classes = new TreeSet<>(this.classes);
      classes.add(x);
      return new Finitary(classes);
    }

    public Finitary multiplyLeft(Finitary x) {
      return AbstractDomain.this.multiply(x, this);
    }

    public Finitary multiply(Finitary y) {
      return AbstractDomain.this.multiply(this, y);
    }

    public Finitary star() {
      return AbstractDomain.this.star(this);
    }

    public Infinitary omega() {
      return AbstractDomain.this.omega(this);
    }

    public Infinitary asInfinitary() {
      Tuple t = new Tuple(neutral(), neutral());
      return AbstractDomain.this.multiply(this, new Infinitary(Collections.singleton(t)));
    }

    public boolean isZero() {
      return this.classes.isEmpty();
    }

    public boolean accepted() {
      return AbstractDomain.this.acceptedFinitary(this);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Finitary that = (Finitary) o;
      return classes.equals(that.classes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(classes);
    }

    @Override
    public String toString() {
      return "{" +
              classes.stream()
                      .map(AbstractDomain.this::monoidElementToString)
                      .collect(Collectors.joining(", "))
              + "}";
    }

    public boolean contain (Finitary x) {
      return classes.containsAll(x.classes);
    }
  }

  /**
   * Represents an abstraction of a language of finite and/or infinite words.
   */
  @Immutable
  public final class Infinitary {

    private final Set<Tuple> tuples;

    private Infinitary() {
      tuples = new TreeSet<>();
    }

    private Infinitary(Set<Tuple> tuples) {
      this.tuples = tuples;
    }

    public Infinitary join(Infinitary x) {
      Set<Tuple> tuples = new TreeSet<>(this.tuples);
      tuples.addAll(x.tuples);
      return new Infinitary(tuples);
    }

    public Infinitary multiplyLeft(Finitary x) {
      return multiply(x, this);
    }

    public boolean accepted() {
      return AbstractDomain.this.acceptedInfinitary(this);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Infinitary that = (Infinitary) o;
      return tuples.equals(that.tuples);
    }

    @Override
    public int hashCode() {
      return Objects.hash(tuples);
    }

    @Override
    public String toString() {
      return "{" +
              tuples.stream()
                      .map(Object::toString)
                      .collect(Collectors.joining(", "))
              + "}";
    }
  }

  protected class Tuple implements Comparable<Tuple> {
    private final int c;
    private final int d;

    public Tuple(int c, int d) {
      this.c = c;
      this.d = d;
    }

    public int getC() {
      return c;
    }

    public int getD() {
      return d;
    }

    @Override
    public String toString() {
      // When D is neutral, the tuple represents finite words
      // then shows only C. ???
//      if (getD() == neutral()) {
//        return AbstractDomain.this.monoidElementToString(getC());
//      } else {
//        return String.format("(%s, %s)", AbstractDomain.this.monoidElementToString(getC()),
//                AbstractDomain.this.monoidElementToString(getD()));
//      }
      return String.format("(%s, %s)", AbstractDomain.this.monoidElementToString(getC()),
              AbstractDomain.this.monoidElementToString(getD()));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Tuple tuple = (Tuple) o;
      return c == tuple.c &&
              d == tuple.d;
    }

    @Override
    public int hashCode() {
      return Objects.hash(c, d);
    }

    @Override
    public int compareTo(Tuple tuple) {
      int x = Integer.compare(this.c, tuple.c);
      return (x == 0) ? Integer.compare(this.d, tuple.d) : x;
    }
  }
}
