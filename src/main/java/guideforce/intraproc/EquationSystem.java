package guideforce.intraproc;

import guideforce.policy.AbstractDomain.Finitary;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Represents a set of equations of the form <code>x = a.y + b.z + c</code>.
 * <p>
 * The left-hand-side of each equation is a variable and the right-hand-side
 * is an effect term. For each variable <code>x</code> there may be at
 * most one equation with <code>x</code> on the left-hand side.
 */
public class EquationSystem implements Iterable<EquationSystem.Equation> {
  private final Map<Variable, Equation> equations = new HashMap<>();

  /**
   * Adds an equation <code>x = p</code>.
   * Any existing equation for <code>x</code> is overwritten.
   */
  public void put(Equation equation) {
    equations.put(equation.getVariable(), equation);
  }

  /**
   * Adds an equation <code>x = p</code>.
   * Any existing equation for <code>x</code> is overwritten.
   */
  public void put(Variable x, EffectTerm<Variable> p) {
    this.put(new Equation(x, p));
  }

  /**
   * Returns the equation with <code>x</code> as the left-hand-side.
   *
   * @param x variable
   * @return equation for <code>x</code>, <code>null</code> if none exists
   */
  public Equation get(Variable x) {
    return equations.get(x);
  }

  @Override
  @Nonnull
  public Iterator<Equation> iterator() {
    return equations.values().iterator();
  }

  /**
   * Solve the equation system.
   * <p>
   * The equations are modified so that the right-hand-side of each
   * equation becomes an effect term that does not contain any variable
   * that appears on a left-hand side.
   */
  public void solve() {

    Map<Variable, Set<Variable>> uses = computeUses();

    for (Variable x : equations.keySet()) {
      EffectTerm<Variable> xSolution = equations.get(x).getRightHandSide();
      Finitary a = xSolution.getCoefficient(x);
      if (a != null) {
        uses.get(x).remove(x);
        xSolution.addConstant(a.omega());
        xSolution.removeMonomial(x);
        xSolution.multiplyLeft(a.star());
      }
      for (Variable y : uses.get(x)) {
        equations.get(y).getRightHandSide().substitute(x, xSolution);
        xSolution.getCoefficients().keySet().forEach(z -> uses.get(z).add(y));
      }
    }
  }

  /**
   * Returns a HashMap that maps each variable to the set of equations
   * (identified by their left-hand-side) in which it is used on the
   * right-hand side. All variables are mapped.
   *
   * <p>
   * For the equations
   * <pre>
   * x = a.y + b
   * y = b.y + c
   * </pre>
   * one would get <code>[x -> [], y -> [x, y]]</code>.
   */
  private HashMap<Variable, Set<Variable>> computeUses() {
    HashMap<Variable, Set<Variable>> uses = new HashMap<>();

    for (Equation e : equations.values()) {
      Variable x = e.getVariable();
      EffectTerm<Variable> p = e.getRightHandSide();
      uses.computeIfAbsent(x, k -> new HashSet<>());
      for (Variable y : p.getCoefficients().keySet()) {
        uses.computeIfAbsent(y, k -> new HashSet<>());
        uses.get(y).add(x);
      }
    }
    return uses;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    equations.forEach((k, e) -> sb.append(e).append("\n"));
    return sb.toString();
  }

  /**
   * Represents an equation <code>x = p</code> of a variable
   * <code>x</code> and an effect term <code>p</code>.
   */
  @Immutable
  public static final class Equation {
    private final Variable variable;
    private final EffectTerm<Variable> rightHandSide;

    public Equation(Variable variable, EffectTerm<Variable> rightHandSide) {
      this.variable = Objects.requireNonNull(variable);
      this.rightHandSide = Objects.requireNonNull(rightHandSide);
    }

    public Variable getVariable() {
      return variable;
    }

    public EffectTerm<Variable> getRightHandSide() {
      return rightHandSide;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Equation equation = (Equation) o;
      return variable.equals(equation.variable) &&
              rightHandSide.equals(equation.rightHandSide);
    }

    @Override
    public int hashCode() {
      return Objects.hash(variable, rightHandSide);
    }

    @Override
    public String toString() {
      return variable + " = " + rightHandSide;
    }
  }
}
