package guideforce.intraproc;

import guideforce.policy.AbstractDomain;
import guideforce.regions.Region;
import guideforce.types.Monad;
import soot.Local;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class FinitaryEffectFlow {
  private Monad<Environment> e;
  private final AbstractDomain domain;

  private FinitaryEffectFlow(AbstractDomain abstractDomain) {
    this.domain = abstractDomain;
  }

  public FinitaryEffectFlow(AbstractDomain domain, Monad<Environment> e) {
    this.domain = domain;
    this.e = e;
  }

  static FinitaryEffectFlow singleton(AbstractDomain abstractDomain) {
    FinitaryEffectFlow result = new FinitaryEffectFlow(abstractDomain);
    result.e = Monad.pure(result.domain, new Environment());
    return result;
  }

  static FinitaryEffectFlow empty(AbstractDomain abstractDomain) {
    FinitaryEffectFlow result = new FinitaryEffectFlow(abstractDomain);
    result.e = Monad.empty(result.domain);
    return result;
  }

  static void copy(FinitaryEffectFlow from, FinitaryEffectFlow to) {
    to.e = from.e; // Monad is immutable
  }

  static void merge(FinitaryEffectFlow in1, FinitaryEffectFlow in2, FinitaryEffectFlow out) {
    out.e = in1.e.join(in2.e);
  }

  public void append(Function<Environment, Monad<Environment>> f) {
    this.e = this.e.then(f);
  }

  public Monad<Environment> get() {
    return e;
  }

  public Monad<Region> thenGet(Local v) {
    return e.then(f -> {
      Region r = f.get(v);
      if (r == null) {
        return Monad.empty(domain);
      } else {
        return Monad.pure(domain, r);
      }
    });
  }

  public String toString() {
    return e.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FinitaryEffectFlow that = (FinitaryEffectFlow) o;
    return e.equals(that.e);
  }

  @Override
  public int hashCode() {
    return Objects.hash(e);
  }
}
