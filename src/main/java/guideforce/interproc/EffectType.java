package guideforce.interproc;

import guideforce.intraproc.EffectTerm;
import guideforce.intraproc.Variable;
import guideforce.policy.AbstractDomain;
import guideforce.policy.AbstractDomain.Infinitary;
import guideforce.regions.Region;
import guideforce.types.Monad;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

@Immutable
public class EffectType {
  private final Monad<Region> type;
  private final Monad<Region> exceptionalType;
  private final EffectTerm<Variable> infinitary;

  public EffectType(Monad<Region> t, Monad<Region> et, Infinitary i) {
    type = Objects.requireNonNull(t);
    exceptionalType = Objects.requireNonNull(et);
    infinitary = new EffectTerm<>(Objects.requireNonNull(i));
  }

  public EffectType(Monad<Region> t, Monad<Region> et, EffectTerm<Variable> p) {
    type = Objects.requireNonNull(t);
    exceptionalType = Objects.requireNonNull(et);
    infinitary = p;
  }

  public Monad<Region> getType() {
    return type;
  }

  public Monad<Region> getExceptionalType() {
    return exceptionalType;
  }

  public EffectTerm<Variable> getInfinitary() {
    return infinitary;
  }

  public AbstractDomain.Finitary getAggregateFinitary() {
    return type.join(exceptionalType).getAggregateFinitary();
  }

  public EffectType join(EffectType other) {
    Monad<Region> newT = this.type.join(other.type);
    Monad<Region> newET = this.exceptionalType.join(other.exceptionalType);
    EffectTerm<Variable> newI = this.infinitary.copy();
    newI.add(other.infinitary);
    return new EffectType(newT, newET, newI);
  }

  @Override
  public String toString() {
    return "(" + type + ") throws (" + exceptionalType + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EffectType that = (EffectType) o;
    return type.equals(that.type) && exceptionalType.equals(that.exceptionalType) && infinitary.equals(that.infinitary);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, exceptionalType, infinitary);
  }
}
