package guideforce.intraproc;

import soot.Timers;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.Orderer;
import soot.toolkits.graph.PseudoTopologicalOrderer;

import java.util.*;

public abstract class ForwardFlowAnalysis<N, A> {
  /** Maps graph nodes to IN sets. */
  protected Map<N, A> unitToBeforeFlow;

  /** The graph being analysed. */
  protected DirectedGraph<N> graph;

  /**
   * Construct the analysis from a DirectedGraph representation of a Body.
   */
  public ForwardFlowAnalysis(DirectedGraph<N> graph) {
    this.graph = graph;
    this.unitToBeforeFlow = new IdentityHashMap<N, A>(graph.size() * 2 + 1);
  }

  /**
   * Default implementation constructing a PseudoTopologicalOrderer.
   *
   * @return an Orderer to order the nodes for the fixed-point iteration
   */
  protected Orderer<N> constructOrderer() {
    return new PseudoTopologicalOrderer<N>();
  }

  /**
   * Returns the flow object corresponding to the initial values for each graph node.
   */
  protected abstract A newInitialFlow();

  /**
   * Returns the initial flow value for entry/exit graph nodes.
   */
  protected abstract A entryInitialFlow();

  /** Creates a copy of the <code>source</code> flow object in <code>dest</code>. */
  protected abstract void copy(A source, A dest);

  /**
   * Compute the merge of the <code>in1</code> and <code>in2</code> sets, putting the result into <code>out</code>. The
   * behavior of this function depends on the implementation ( it may be necessary to check whether <code>in1</code> and
   * <code>in2</code> are equal or aliased ). Used by the doAnalysis method.
   */
  protected abstract void merge(A in1, A in2, A out);

  protected void doAnalysis() {
    List<N> orderedUnits = constructOrderer().newList(graph, false);

    final int n = orderedUnits.size();
    BitSet work = new BitSet(n);
    work.set(0, n);

    final Map<N, Integer> index = new IdentityHashMap<N, Integer>(n * 2 + 1);
    {
      int i = 0;
      for (N s : orderedUnits) {
        index.put(s, i++);

        // Set initial Flows
        unitToBeforeFlow.put(s, newInitialFlow());
      }
    }

    for (N s : graph.getHeads()) {
      unitToBeforeFlow.put(s, entryInitialFlow());
    }

    int numComputations = 0;

    // Perform fixed point flow analysis
    {
      for (int i = work.nextSetBit(0); i >= 0; i = work.nextSetBit(i + 1)) {
        work.clear(i);
        N s = orderedUnits.get(i);

        // Compute the out-flows of s
        A beforeFlow = unitToBeforeFlow.get(s);
        Map<N, A> afterFlows = flowThrough(beforeFlow, s);

        // If the out-flow is changed, merge it into unitToBeforeFlow
        for (Map.Entry<N, A> entry : afterFlows.entrySet()) {
          N to = entry.getKey();
          A afterFlow = entry.getValue();
          A previousFlow = unitToBeforeFlow.get(to);
          A mergedFlow = newInitialFlow();
          merge(afterFlow,previousFlow,mergedFlow);
          boolean hasChanged = !previousFlow.equals(mergedFlow);
          if (hasChanged) {
            unitToBeforeFlow.put(to,mergedFlow);
            int j = index.get(to);
            work.set(j);
            i = Math.min(i, j - 1);
          }
        }
        numComputations++;
      }
    }

    Timers.v().totalFlowNodes += n;
    Timers.v().totalFlowComputations += numComputations;
  }

  /** Accessor function returning value of IN set for s. */
  public A getFlowBefore(N s) {
    A a = unitToBeforeFlow.get(s);
    return a == null ? newInitialFlow() : a;
  }

  /** Accessor function returning value of OUT set for s. */
  public A getFlowAfter(N s) {
    A a = getFlowBefore(s);
    final Iterator<N> it = graph.getSuccsOf(s).iterator();
    if (it.hasNext()) {
      a = unitToBeforeFlow.get(it.next());
       while (it.hasNext()) {
         merge(a , unitToBeforeFlow.get(it.next()), a);
       }
    }
    return a;
  }

  /**
   * Returns the flow through a node. The returned map specifies the target nodes of the
   * flows and the flow that goes to that node.
   */
  protected abstract Map<N, A> flowThrough(A in, N node);

}
