package guideforce.policy;

import guideforce.regions.SpecialRegion;
import guideforce.types.Monad;
import soot.SootMethodRef;
import soot.VoidType;
import soot.jimple.AnyNewExpr;
import soot.jimple.StringConstant;
import guideforce.interproc.CallingContext;
import guideforce.interproc.Location;
import guideforce.policy.AbstractDomain.Finitary;
import guideforce.policy.automata.Automaton;
import guideforce.policy.automata.AutomatonAbstractDomain;
import guideforce.regions.AllocationSiteRegion;
import guideforce.regions.MonoidRegion;
import guideforce.regions.Region;

import javax.annotation.Nonnull;
import java.util.List;

public class BinaryPolicy implements Policy {
  private final AutomatonAbstractDomain abstractDomain;

  public BinaryPolicy() {
    Automaton a = new Automaton();

    a.addAlphabetSymbol(Token.U);
    a.addAlphabetSymbol(Token.T);

    a.addState("U");
    a.addState("T");

    a.addEdge("U", "U", Token.U);
    a.addEdge("U", "T", Token.T);
    a.addEdge("T", "T", Token.U);
    a.addEdge("T", "T", Token.T);

    a.setInitialState("U");
    a.addFinalState("U");

    this.abstractDomain = new AutomatonAbstractDomain(a);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return this.abstractDomain;
  }

  @Override
  public Region getRegion(CallingContext ctx, Location location, StringConstant c) {
    return new MonoidRegion(abstractDomain, abstractDomain.read(Token.U));
  }

  @Override
  public Region getRegion(CallingContext ctx, Location location, AnyNewExpr expr) {
    return new AllocationSiteRegion(expr, ctx, location);
  }

  @Override
  public Intrinsic getIntrinsicMethod(SootMethodRef method) {
    // TODO: Write specific tests for all cases.
    switch (method.getSignature()) {
      case "<java.lang.String: void <init>()>":
        return new Intrinsic() {
          @Nonnull
          @Override
          public Monad<Region> getReturnType(Region region, List<Region> argumentTypes) {
            Region regionU = new MonoidRegion(abstractDomain, abstractDomain.read(Token.U));
            return Monad.pure(abstractDomain, regionU);
          }

          @Nonnull
          @Override
          public Monad<Region> getExceptionalType(Region region, List<Region> argumentTypes) {
            return Monad.empty(abstractDomain);
          }
        };
      case "<java.lang.String: void <init>(java.lang.String)>":
        return new Intrinsic() {
          @Nonnull
          @Override
          public Monad<Region> getReturnType(Region region, List<Region> refinedArgs) {
            assert refinedArgs.size() == 1;
            return Monad.pure(abstractDomain, refinedArgs.get(0));
          }

          @Nonnull
          @Override
          public Monad<Region> getExceptionalType(Region region, List<Region> argumentTypes) {
            return Monad.empty(abstractDomain);
          }
        };
      case "<java.lang.String: java.lang.String concat(java.lang.String)>":
      case "<java.lang.String: java.lang.String valueOf(java.lang.String)>":
        return new Intrinsic() {
          @Nonnull
          @Override
          public Monad<Region> getReturnType(Region region, List<Region> refinedArgs) {
            assert refinedArgs.size() == 1;
            return Monad.pure(abstractDomain, region).join(Monad.pure(abstractDomain, refinedArgs.get(0)));
          }

          @Nonnull
          @Override
          public Monad<Region> getExceptionalType(Region region, List<Region> argumentTypes) {
            return Monad.empty(abstractDomain);
          }
        };
      case "<java.lang.String: java.lang.String replace(char,char)>":
      case "<java.lang.String: java.lang.String substring(int)>":
      case "<java.lang.String: java.lang.String substring(int,int)>":
      case "<java.lang.String: java.lang.String toLowerCase()>":
      case "<java.lang.String: java.lang.String toLowerCase(java.util.Locale)>":
      case "<java.lang.String: java.lang.String toUpperCase()>":
      case "<java.lang.String: java.lang.String toUpperCase(java.util.Locale)>":
      case "<java.lang.Object: java.lang.String toString()>":
      case "<java.lang.String: java.lang.String toString()>":
        return new Intrinsic() {
          @Nonnull
          @Override
          public Monad<Region> getReturnType(Region region, List<Region> argumentTypes) {
            return Monad.pure(abstractDomain, region);
          }

          @Nonnull
          @Override
          public Monad<Region> getExceptionalType(Region region, List<Region> argumentTypes) {
            return Monad.empty(abstractDomain);
          }
        };
      case "<ourlib.nonapp.TaintAPI: void outputString(java.lang.String)>":
        return new Intrinsic() {
          @Nonnull
          @Override
          public Monad<Region> getReturnType(Region region, List<Region> refinedArgs) {
            assert method.getReturnType().equals(VoidType.v());
            return Monad.pure(abstractDomain, (Region) SpecialRegion.BASETYPE_REGION)
                    .then(getEffect(region, refinedArgs));
          }

          @Nonnull
          @Override
          public Monad<Region> getExceptionalType(Region region, List<Region> argumentTypes) {
            return Monad.empty(abstractDomain);
          }

          @Nonnull
          public Finitary getEffect(Region region, List<Region> argumentTypes) {
            // outputString has exactly the effects that are included in the regions of its
            // argument.
            assert (argumentTypes.size() == 1);
            Region arg1 = argumentTypes.get(0);
            Finitary effect = abstractDomain.oneFinitary();
            // argument is atomic, so this loop will be taken exactly once
              if (arg1 instanceof MonoidRegion) {
                effect = ((MonoidRegion) arg1).asFinitary();
              }
            return effect;
          }
        };
      //case "<java.net.URLDecoder: java.lang.String decode(java.lang.String,java.lang.String)>":
      case "<ourlib.nonapp.TaintAPI: java.lang.String getTaintedString()>":
        return new Intrinsic() {
          @Nonnull
          @Override
          public Monad<Region> getReturnType(Region region, List<Region> argumentTypes) {
            Region regionT = new MonoidRegion(abstractDomain, abstractDomain.read(Token.T));
            return Monad.pure(abstractDomain, regionT)
                    .then(getEffect(region, argumentTypes));
          }

          @Nonnull
          @Override
          public Monad<Region> getExceptionalType(Region region, List<Region> argumentTypes) {
            return Monad.empty(abstractDomain);
          }

          @Nonnull
          public Finitary getEffect(Region region, List<Region> argumentTypes) {
            return abstractDomain.oneFinitary();
          }
        };
      default:
        return null;
    }
  }

  public enum Token {
    U, T
  }
}
