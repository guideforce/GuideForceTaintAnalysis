package guideforce.interproc;

import soot.SootMethod;
import soot.jimple.Stmt;

import javax.annotation.concurrent.Immutable;
import java.util.LinkedList;
import java.util.Objects;

@Immutable
public final class CallingContext {
  private final int k;
  private final LinkedList<Location> callString;

  public CallingContext(int k) {
    this.k = k;
    this.callString = new LinkedList<>();
  }

  CallingContext(CallingContext ctx) {
    Objects.requireNonNull(ctx);
    this.k = ctx.k;
    this.callString = new LinkedList<>(ctx.callString);
  }

  public CallingContext push(SootMethod m, Stmt s) {
    CallingContext ctx = new CallingContext(this);
    if (ctx.k > 0) {
      ctx.callString.addLast(new Location(m, s));
      if (ctx.callString.size() > ctx.k) {
        ctx.callString.removeFirst();
      }
    }
    return ctx;
  }

  public String toString() {
    StringBuilder str = new StringBuilder();
    for (Location p : callString) {
      str.append("in call from ").append(p);
    }
    return str.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CallingContext that = (CallingContext) o;
    return k == that.k &&
            callString.equals(that.callString);
  }

  @Override
  public int hashCode() {
    return Objects.hash(k, callString);
  }
}
