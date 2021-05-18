package guideforce.policy.automata;

import java.util.*;

class TransitionBox {
  final Map<Automaton.State, Map<Automaton.State, Behaviour>> actions = new HashMap<>();

  List<Object> representant;

  private TransitionBox() {
  }

  static TransitionBox neutral(List<Automaton.State> states, List<Automaton.State> finalStates) {
    TransitionBox output = new TransitionBox();
    output.representant = Collections.emptyList();
    for (Automaton.State state1 : states) {
      Map<Automaton.State, Behaviour> map = new HashMap<>();
      for (Automaton.State state2 : states) {
        if (state1.equals(state2)) {
          if (finalStates.contains(state2)) {
            map.put(state2, Behaviour.ReachableThroughFinal);
          } else {
            map.put(state2, Behaviour.Reachable);
          }
        } else {
          map.put(state2, Behaviour.NotReachable);
        }
      }
      output.actions.put(state1, map);
    }
    return output;
  }

  static TransitionBox empty(List<Automaton.State> states, List<Object> representant) {
    TransitionBox output = new TransitionBox();
    output.representant = representant;
    for (Automaton.State state1 : states) {
      Map<Automaton.State, Behaviour> map = new HashMap<>();
      for (Automaton.State state2 : states) {
        map.put(state2, Behaviour.NotReachable);
      }
      output.actions.put(state1, map);
    }
    return output;
  }

  public static TransitionBox concat(TransitionBox a, TransitionBox b) {
    ArrayList<Automaton.State> states = new ArrayList<>(a.actions.keySet());
    List<Object> representant = new LinkedList<>(a.representant);
    representant.addAll(b.representant);
    TransitionBox c = TransitionBox.empty(states, representant);

    for (Automaton.State state1 : states) {
      for (Automaton.State state2 : states) {
        for (Automaton.State state3 : states) {
          Behaviour current = c.actions.get(state1).get(state3);
          if (current == Behaviour.ReachableThroughFinal) continue;

          Behaviour behaviourA12 = a.actions.get(state1).get(state2);
          Behaviour behaviourB23 = b.actions.get(state2).get(state3);

          if (behaviourA12 == Behaviour.NotReachable || behaviourB23 == Behaviour.NotReachable)
            continue;
          if (behaviourA12 == Behaviour.ReachableThroughFinal || behaviourB23 == Behaviour.ReachableThroughFinal)
            c.actions.get(state1).put(state3, Behaviour.ReachableThroughFinal);
          else
            c.actions.get(state1).put(state3, Behaviour.Reachable);
        }
      }
    }

    return c;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TransitionBox that = (TransitionBox) o;
    return actions.equals(that.actions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actions);
  }

  @Override
  public String toString() {
    return String.valueOf(this.representant);
  }

  public enum Behaviour {
    NotReachable,
    Reachable,
    ReachableThroughFinal
  }
}
