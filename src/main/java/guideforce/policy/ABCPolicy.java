package guideforce.policy;

import guideforce.interproc.CallingContext;
import guideforce.interproc.Location;
import guideforce.policy.automata.Automaton;
import guideforce.policy.automata.AutomatonAbstractDomain;
import guideforce.regions.AllocationSiteRegion;
import guideforce.regions.Region;
import guideforce.regions.SpecialRegion;
import guideforce.types.Monad;
import soot.SootMethodRef;
import soot.VoidType;
import soot.jimple.AnyNewExpr;
import soot.jimple.StringConstant;

import javax.annotation.Nonnull;
import java.util.List;

public class ABCPolicy implements Policy {
  private final AutomatonAbstractDomain abstractDomain;

  public ABCPolicy() {
    Automaton a = new Automaton();

    a.addAlphabetSymbol(Token.A);
    a.addAlphabetSymbol(Token.B);
    a.addAlphabetSymbol(Token.C);

    a.addState("s0");
    a.addState("sa");
    a.addState("saa");
    a.addState("sab");
    a.addState("sac");
    a.addState("sb");
    a.addState("sba");
    a.addState("sbb");
    a.addState("sbc");
    a.addState("sc");
    a.addState("sca");
    a.addState("scb");
    a.addState("scc");
    a.addState("saaa");
    a.addState("sbbb");
    a.addState("sccc");
    a.addState("s1");

    a.addEdge("s0", "sa", Token.A);
    a.addEdge("s0", "sb", Token.B);
    a.addEdge("s0", "sc", Token.C);

    a.addEdge("sa", "saa", Token.A);
    a.addEdge("sa", "sab", Token.B);
    a.addEdge("sa", "sac", Token.C);

    a.addEdge("sb", "sba", Token.A);
    a.addEdge("sb", "sbb", Token.B);
    a.addEdge("sb", "sbc", Token.C);

    a.addEdge("sc", "sca", Token.A);
    a.addEdge("sc", "scb", Token.B);
    a.addEdge("sc", "scc", Token.C);

    a.addEdge("saa", "saaa", Token.A);
    a.addEdge("saa", "s1", Token.B);
    a.addEdge("saa", "s1", Token.C);
    a.addEdge("sab", "s1", Token.A);
    a.addEdge("sab", "s1", Token.B);
    a.addEdge("sab", "s1", Token.C);
    a.addEdge("sac", "s1", Token.A);
    a.addEdge("sac", "s1", Token.B);
    a.addEdge("sac", "s1", Token.C);

    a.addEdge("sba", "s1", Token.A);
    a.addEdge("sba", "s1", Token.B);
    a.addEdge("sba", "s1", Token.C);
    a.addEdge("sbb", "s1", Token.A);
    a.addEdge("sbb", "sbbb", Token.B);
    a.addEdge("sbb", "s1", Token.C);
    a.addEdge("sbc", "s1", Token.A);
    a.addEdge("sbc", "s1", Token.B);
    a.addEdge("sbc", "s1", Token.C);

    a.addEdge("sca", "s1", Token.A);
    a.addEdge("sca", "s1", Token.B);
    a.addEdge("sca", "s1", Token.C);
    a.addEdge("scb", "s1", Token.A);
    a.addEdge("scb", "s1", Token.B);
    a.addEdge("scb", "s1", Token.C);
    a.addEdge("scc", "s1", Token.A);
    a.addEdge("scc", "s1", Token.B);
    a.addEdge("scc", "sccc", Token.C);

    a.addEdge("saaa", "saaa" , Token.A);
    a.addEdge("saaa", "s1" , Token.B);
    a.addEdge("saaa", "s1" , Token.C);
    a.addEdge("sbbb", "s1" , Token.A);
    a.addEdge("sbbb", "sbbb" , Token.B);
    a.addEdge("sbbb", "s1" , Token.C);
    a.addEdge("sccc", "s1" , Token.A);
    a.addEdge("sccc", "s1" , Token.B);
    a.addEdge("sccc", "sccc" , Token.C);

    a.addEdge("s1", "s1", Token.A);
    a.addEdge("s1", "s1", Token.B);
    a.addEdge("s1", "s1", Token.C);

    a.setInitialState("s0");
    a.addFinalState("s0");
    a.addFinalState("sa");
    a.addFinalState("saa");
    a.addFinalState("sab");
    a.addFinalState("sac");
    a.addFinalState("sb");
    a.addFinalState("sba");
    a.addFinalState("sbb");
    a.addFinalState("sbc");
    a.addFinalState("sc");
    a.addFinalState("sca");
    a.addFinalState("scb");
    a.addFinalState("scc");
    a.addFinalState("saaa");
    a.addFinalState("sbbb");
    a.addFinalState("sccc");
    a.addFinalState("s1");

    this.abstractDomain = new AutomatonAbstractDomain(a);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return this.abstractDomain;
  }

  @Override
  public Region getRegion(CallingContext ctx, Location location, StringConstant v) {
    return new AllocationSiteRegion(v, ctx, location);
  }

  @Override
  public Region getRegion(CallingContext ctx, Location location, AnyNewExpr v) {
    return new AllocationSiteRegion(v, ctx, location);
  }

  @Override
  public Intrinsic getIntrinsicMethod(SootMethodRef method) {
    switch (method.getSignature()) {
      case "<ourlib.nonapp.TaintAPI: void emitA()>":
        return emitIntrinsic(method, Token.A);
      case "<ourlib.nonapp.TaintAPI: void emitB()>":
        return emitIntrinsic(method, Token.B);
      case "<ourlib.nonapp.TaintAPI: void emitC()>":
        return emitIntrinsic(method, Token.C);
      default:
        return null;
    }
  }

  private Intrinsic emitIntrinsic(SootMethodRef method, Token token) {
    return new Intrinsic() {
      @Nonnull
      @Override
      public Monad<Region> getReturnType(Region region, List<Region> argumentTypes) {
        assert method.getReturnType().equals(VoidType.v());
        return Monad.pure(abstractDomain, (Region)SpecialRegion.BASETYPE_REGION)
                .then(abstractDomain.makeFinitary(abstractDomain.read(token)));
      }

      @Nonnull
      @Override
      public Monad<Region> getExceptionalType(Region region, List<Region> argumentTypes) {
        return Monad.empty(abstractDomain);
      }
    };
  }

  public enum Token {
    A, B, C
  }
}
