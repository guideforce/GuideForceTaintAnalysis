package guideforce.intraproc;

import guideforce.policy.AbstractDomain;
import guideforce.policy.AbstractDomain.Finitary;
import guideforce.policy.AbstractDomain.Infinitary;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents terms of the form <code>a.x + b.y + c</code>,
 * where <code>x,y</code> are <i>variables</i> of type <code>K</code>,
 * the <i>coefficients</i> <code>a, b</code> are finitary abstractions, and the
 * <i>constant term</i> <code>d</code> is an intraprocedural abstraction.
 *
 * @param <K> type of variables
 */
public class EffectTerm<K> {
  private final Map<K, Finitary> coefficients;
  private Infinitary constantTerm;

  /**
   * Construct zero effect term
   */
  public EffectTerm(AbstractDomain abstractDomain) {
    this.coefficients = new HashMap<>();
    this.constantTerm = abstractDomain.zeroInfinitary();
  }

  /**
   * Construct effect term with given constant
   */
  public EffectTerm(Infinitary infinitary) {
    this.coefficients = new HashMap<>();
    this.constantTerm = infinitary;
  }

  /**
   * Copy constructor
   */
  private EffectTerm(EffectTerm<K> other) {
    this.coefficients = new HashMap<>();
    for (Map.Entry<K, Finitary> entry : other.coefficients.entrySet()) {
      this.coefficients.put(entry.getKey(), entry.getValue());
    }
    this.constantTerm = other.constantTerm;
  }

  public Infinitary getConstantTerm() {
    return constantTerm;
  }

  /**
   * Returns an unmodifiable map of the coefficients
   */
  public Map<K, Finitary> getCoefficients() {
    return Collections.unmodifiableMap(coefficients);
  }

  public Finitary getCoefficient(K k) {
    return coefficients.get(k);
  }

  /**
   * Modify the effect term by adding a constant
   */
  public void addConstant(Infinitary c) {
    this.constantTerm = this.constantTerm.join(c);
  }

  /**
   * Modify the effect term by adding <code>a.k</code>.
   */
  public void addMonomial(K k, Finitary a) {
    coefficients.compute(k, (k1, v) -> (v == null) ? a : v.join(a));
  }

  /**
   * Modify by removing any coefficient of the form <code>a.k</code>.
   */
  public void removeMonomial(K k) {
    coefficients.remove(k);
  }

  /**
   * Modify the effect term by substituting the variable <code>k</code>
   * with the given effect term <code>p</code>.
   * The effect term <code>p</code> is not modified.
   */
  public void substitute(K k, EffectTerm<K> p) {
    Finitary a = getCoefficient(k);
    if (a == null) {
      return;
    }
    EffectTerm<K> pk = p.copy();
    pk.multiplyLeft(a);
    this.removeMonomial(k);
    this.add(pk);
  }

  /**
   * Modify the effect term by adding another effect term <code>p</code>.
   * The other effect term <code>p</code> is not modified.
   */
  public void add(EffectTerm<K> other) {
    other.coefficients.forEach(this::addMonomial);
    this.constantTerm = this.constantTerm.join(other.constantTerm);
  }

  /**
   * Modify the effect term by multiplying each term from the left
   * with <code>a</code>.
   * Thus, <code>b.x + c.y + d</code> becomes
   * <code>a.b.x + a.c.y + a.d</code>.
   */
  public void multiplyLeft(Finitary a) {
    this.coefficients.replaceAll((k, v) -> v.multiplyLeft(a));
    this.constantTerm = this.constantTerm.multiplyLeft(a);
  }

  /**
   * Return a copy of the effect term
   */
  public EffectTerm<K> copy() {
    return new EffectTerm<>(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EffectTerm<?> that = (EffectTerm<?>) o;
    return coefficients.equals(that.coefficients) &&
            constantTerm.equals(that.constantTerm);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coefficients, constantTerm);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    if (!coefficients.isEmpty()) {
      sb.append(coefficients.entrySet().stream()
              .map(e -> e.getValue().toString() + "." + e.getKey().toString())
              .collect(Collectors.joining(" + ")));
      sb.append(" + ");
    }
    sb.append(constantTerm.toString());
    return sb.toString();
  }
}
