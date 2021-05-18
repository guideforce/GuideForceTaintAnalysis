package guideforce.policy.automata;

import guideforce.policy.AbstractDomain;

import java.util.*;

public class AutomatonAbstractDomain extends AbstractDomain {
  private final SyntacticMonoid monoid;
  private final Map<Object, Integer> alphabetClasses;
  private final Set<Integer> acceptedFinitary;

  private final Set<Tuple> tuples;
  private final Set<Tuple> acceptedInfinitary;

  // TODO: it's probably better to pre-compute this data
  private final Map<Tuple, Set<Tuple>> singletonClosures = new HashMap<>();
  private final Map<Tuple, HashMap<Tuple, Boolean>> intersect = new HashMap<>();


  public AutomatonAbstractDomain(Automaton automaton) {
    this.monoid = automaton.toMonoid();

    this.alphabetClasses = new HashMap<>();
    for (Map.Entry<Object, TransitionBox> entry : automaton.computeAlphabetClasses().entrySet()) {
      this.alphabetClasses.put(entry.getKey(), this.monoid.elements().indexOf(entry.getValue()));
    }

    acceptedFinitary = new HashSet<>();
    for (int i = 0; i < monoid.elements().size(); i++) {
      if (automaton.acceptsWord(monoid.elements().get(i).representant)) {
        acceptedFinitary.add(i);
      }
    }

    tuples = new HashSet<>();
    for (int C = 0; C < monoid.elements().size(); C++) {
      for (int D = 0; D < monoid.elements().size(); D++) {
        if (monoid.multiply(C, D) == C && monoid.multiply(D, D) == D) {
          tuples.add(new Tuple(C, D));
        }
      }
    }

    acceptedInfinitary = new HashSet<>();
    for (Tuple tuple : tuples) {
//      singletonClosures.put(tuple, computeClosure(tuple));

      if (tuple.getD() == monoid.neutral()) {
        if (automaton.acceptsWord(monoid.elements().get(tuple.getC()).representant)) {
          acceptedInfinitary.add(tuple);
        }
      } else {
        Automaton tupleAutomaton = concretize(tuple);
        if (!Automaton.isIntersectionBuechiEmpty(tupleAutomaton, automaton)) {
          acceptedInfinitary.add(tuple);
        }
      }
    }

    //acceptedInfinitary = closure(acceptedInfinitary); bereits abgeschlossen nach Lemma 2.9 (c)
  }

  @Override
  public int neutral() {
    return monoid.neutral();
  }

  @Override
  public int multiply(int x, int y) {
    return monoid.multiply(x, y);
  }

  @Override
  public String monoidElementToString(int x) {
    return monoid.elements().get(x).toString();
  }

  @Override
  public Set<Integer> getAllMonoidElements() {
    Set<Integer> all = new TreeSet<>();
    for (int i = 0; i < monoid.elements().size(); i++) {
      all.add(i);
    }
    return all;
  }

  @Override
  protected boolean acceptedFinitary(Set<Integer> x) {
    return acceptedFinitary.containsAll(x);
  }

  @Override
  protected boolean acceptedInfinitary(Set<Tuple> x) {
    return acceptedInfinitary.containsAll(x);
  }

  @Override
  protected Set<Integer> getAcceptedFinitaryClasses() {
    return acceptedFinitary;
  }

  @Override
  protected Set<Tuple> getAcceptedInfinitaryClasses() {
    return acceptedInfinitary;
  }

  @Override
  public int read(Object token) {
    return alphabetClasses.get(token);
  }

  protected Set<Integer> star(Set<Integer> a) {
    Set<Integer> output = new HashSet<>(a);

    do {
      a = new HashSet<>(output);
      output.addAll(a);
      output.add(monoid.neutral());

      for (Integer x : a) {
        for (Integer y : a) {
          output.add(monoid.multiply(x, y));
        }
      }
    } while (!a.equals(output));

    return output;
  }

  private Automaton concretize(Tuple tuple) {
    if (tuple.getD() == monoid.neutral()) {
      throw new IllegalArgumentException("tuple.D must not be the empty class.");
    }

    Automaton a = Automaton.fromMonoid(monoid, alphabetClasses, tuple.getC());
    Automaton b = Automaton.fromMonoid(monoid, alphabetClasses, tuple.getD());

    return Automaton.concat(a, Automaton.omega(b));
  }

  private Set<Tuple> computeClosure(Tuple tuple) {
    Set<Tuple> closure = new HashSet<>();
    closure.add(tuple);

    if (tuple.getD() == monoid.neutral()) {
      return closure;
    }

    boolean fixpoint;
    do {
      fixpoint = true;

      for (Tuple tupleToCheck : tuples) {
        if (closure.contains(tupleToCheck) || tupleToCheck.getD() == monoid.neutral()) {
          continue;
        }

        for (Tuple closureTuple : closure) {
          if (!intersectionEmpty(tupleToCheck, closureTuple)) {
            closure.add(tupleToCheck);
            fixpoint = false;
            break;
          }
        }
      }
    } while (!fixpoint);

    return closure;
  }

  private Boolean intersectionEmpty(Tuple a, Tuple b) {
    if (intersect.containsKey(a) && intersect.get(a).containsKey(b)) {
      return intersect.get(a).get(b);
    }

    Boolean result = Automaton.isIntersectionBuechiEmpty(concretize(a), concretize(b));

    if (!intersect.containsKey(a)) {
      intersect.put(a, new HashMap<>());
    }
    if (!intersect.containsKey(b)) {
      intersect.put(b, new HashMap<>());
    }
    intersect.get(a).put(b, result);
    intersect.get(b).put(a, result);

    return result;
  }

  private Set<Tuple> closure(Tuple tuple) {
    if (!singletonClosures.containsKey(tuple)) {
      singletonClosures.put(tuple, computeClosure(tuple));
    }

    return singletonClosures.get(tuple);
  }

  private Set<Tuple> closure(Set<Tuple> set) {
    Set<Tuple> output = new HashSet<>();
    for (Tuple tuple : set) {
      output.addAll(closure(tuple));
    }
    return output;
  }

  protected Set<Tuple> omega(Set<Integer> classes) {
    Set<Integer> nonEmptyClasses = new HashSet<>();

    for (Integer cls : classes) {
      if (cls != monoid.neutral()) {
        nonEmptyClasses.add(cls);
      }
    }

    Automaton classesAutomaton = Automaton.fromMonoid(monoid, alphabetClasses, nonEmptyClasses);
    classesAutomaton = Automaton.omega(classesAutomaton);

    Set<Tuple> abstraction = new HashSet<>();
    for (Tuple tuple : tuples) {
      if (tuple.getD() == monoid.neutral()) {
        continue;
      }

      Automaton tupleAutomaton = concretize(tuple);
      if (!Automaton.isIntersectionBuechiEmpty(classesAutomaton, tupleAutomaton)) {
        abstraction.add(tuple);
      }
    }

    if (classes.contains(monoid.neutral())) {
      Set<Integer> omega = star(classes);
      for (Integer cls : omega) {
        abstraction.add(new Tuple(cls, monoid.neutral()));
      }
    }

    return closure(abstraction);
  }

  public Set<Tuple> concat(Set<Integer> classes, Set<Tuple> tuples) {
    Set<Tuple> output = new HashSet<>();
    for (Integer cls : classes) {
      for (Tuple tuple : tuples) {
        Tuple concatenation = new Tuple(monoid.multiply(cls, tuple.getC()), tuple.getD());
        output.add(concatenation);
      }
    }
    return output;
  }

}
