package guideforce.interproc;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.tagkit.SourceFileTag;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

@Immutable
public final class Location {
  private final SootMethod method;
  private final Stmt location;

  public Location(SootMethod method, Stmt location) {
    this.method = Objects.requireNonNull(method);
    this.location = Objects.requireNonNull(location);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    SourceFileTag tag = (SourceFileTag) method.getDeclaringClass().getTag("SourceFileTag");
    if (tag != null) {
      sb.append(".(").append(tag.getSourceFile())
              .append(":").append(location.getJavaSourceStartLineNumber()).append(")");
    } else {
      sb.append(method.getDeclaringClass()).append(".").append(method.getName());
      sb.append("(").append(location.getJavaSourceStartLineNumber()).append(")");
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Location location1 = (Location) o;
    return method.equals(location1.method) &&
            location.equals(location1.location);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, location);
  }
}
