package guideforce.policy.automata;

import java.util.*;

public class Automaton {
  private List<State> states = new ArrayList<>();
  private State initialState;
  private Set<Object> alphabet = new HashSet<>();
  private List<State> finalStates = new ArrayList<>();
  private List<Edge> edges = new ArrayList<>();

  static Automaton fromMonoid(SyntacticMonoid syntacticMonoid,
                              Map<Object, Integer> alphabetMonoidRelations, Integer accepting) {
    Set<Integer> acceptingSet = new HashSet<>();
    acceptingSet.add(accepting);
    return fromMonoid(syntacticMonoid, alphabetMonoidRelations, acceptingSet);
  }

  static Automaton fromMonoid(SyntacticMonoid syntacticMonoid,
                              Map<Object, Integer> alphabetMonoidRelations,
                              Set<Integer> accepting) {
    Automaton automaton = new Automaton();
    automaton.alphabet = new HashSet<>(alphabetMonoidRelations.keySet());

    automaton.states = new ArrayList<>();
    Map<TransitionBox, State> classStateMap = new HashMap<>();
    for (TransitionBox cls : syntacticMonoid) {
      State state = new State(cls.representant.toString()); // TODO
      automaton.states.add(state);
      classStateMap.put(cls, state);
    }

    automaton.initialState =
            classStateMap.get(syntacticMonoid.elements().get(syntacticMonoid.neutral()));

    automaton.finalStates = new ArrayList<>();
    for (Integer cls : accepting) {
      automaton.finalStates.add(classStateMap.get(syntacticMonoid.elements().get(cls)));
    }

    automaton.edges = new ArrayList<>();
    for (int i = 0; i < syntacticMonoid.elements().size(); i++) {
      for (Object token : automaton.alphabet) {
        TransitionBox cls = syntacticMonoid.elements().get(i);
        Integer end = syntacticMonoid.multiply(i, alphabetMonoidRelations.get(token));
        Edge edge = new Edge(classStateMap.get(cls), automaton.states.get(end), token);
        automaton.edges.add(edge);
      }
    }

    return automaton;
  }

  public static Automaton intersect(Automaton a, Automaton b) {
    if (!a.alphabet.equals(b.alphabet)) {
      throw new IllegalArgumentException("Automatons to intersect must have the same alphabet.");
    }

    Automaton c = new Automaton();

    c.alphabet = a.alphabet;

    c.states = new ArrayList<>();
    Map<State, Map<State, State[]>> stateMap = new HashMap<>(); //Maps a state of Automaton a and
    // a state of Automaton b to the two corresponding states in the new automaton c
    for (State stateA : a.states) {
      stateMap.put(stateA, new HashMap<>());

      for (State stateB : b.states) {
        State new0 = new State("(" + stateA.Name + "#" + stateB.Name + ").0");
        State new1 = new State("(" + stateA.Name + "#" + stateB.Name + ").1");

        c.states.add(new0);
        c.states.add(new1);
        stateMap.get(stateA).put(stateB, new State[]{new0, new1});
      }
    }

    c.initialState = stateMap.get(a.initialState).get(b.initialState)[0];

    c.edges = new ArrayList<>();
    for (Edge edgeA : a.edges) {
      for (Edge edgeB : b.edges) {
        if (!edgeA.label.equals(edgeB.label)) continue;

        if (a.finalStates.contains(edgeA.start)) {
          c.edges.add(new Edge(
                  stateMap.get(edgeA.start).get(edgeB.start)[0],
                  stateMap.get(edgeA.end).get(edgeB.end)[1],
                  edgeA.label));
        } else {
          c.edges.add(new Edge(
                  stateMap.get(edgeA.start).get(edgeB.start)[0],
                  stateMap.get(edgeA.end).get(edgeB.end)[0],
                  edgeA.label));
        }

        if (b.finalStates.contains(edgeB.start)) {
          c.edges.add(new Edge(
                  stateMap.get(edgeA.start).get(edgeB.start)[1],
                  stateMap.get(edgeA.end).get(edgeB.end)[0],
                  edgeA.label));
        } else {
          c.edges.add(new Edge(
                  stateMap.get(edgeA.start).get(edgeB.start)[1],
                  stateMap.get(edgeA.end).get(edgeB.end)[1],
                  edgeA.label));
        }
      }
    }

    c.finalStates = new ArrayList<>();
    for (State stateA : a.states) {
      for (State finalB : b.finalStates) {
        c.finalStates.add(stateMap.get(stateA).get(finalB)[1]);
      }
    }

    return c;
  }

  static Boolean isIntersectionBuechiEmpty(Automaton a, Automaton b) {
    if (!a.alphabet.equals(b.alphabet)) {
      throw new IllegalArgumentException("Automatons to intersect must have the same alphabet.");
    }

    Automaton c = new Automaton();

    c.states = new ArrayList<>();
    Map<State, Map<State, State[]>> stateMap = new HashMap<>(); //Maps a state of Automaton a and
    // a state of Automaton b to the two corresponding states in the new automaton c
    Map<State, Set<State>> directlyReachable = new HashMap<>();
    for (State stateA : a.states) {
      stateMap.put(stateA, new HashMap<>());

      for (State stateB : b.states) {
        State new0 = new State("(" + stateA.Name + "#" + stateB.Name + ").0");
        State new1 = new State("(" + stateA.Name + "#" + stateB.Name + ").1");

        c.states.add(new0);
        c.states.add(new1);
        stateMap.get(stateA).put(stateB, new State[]{new0, new1});

        directlyReachable.put(new0, new HashSet<>());
        directlyReachable.put(new1, new HashSet<>());
      }
    }

    c.initialState = stateMap.get(a.initialState).get(b.initialState)[0];

    for (Edge edgeA : a.edges) {
      for (Edge edgeB : b.edges) {
        if (!edgeA.label.equals(edgeB.label)) continue;

        if (a.finalStates.contains(edgeA.start)) {
          directlyReachable.get(stateMap.get(edgeA.start).get(edgeB.start)[0]).add(stateMap.get(edgeA.end).get(edgeB.end)[1]);
        } else {
          directlyReachable.get(stateMap.get(edgeA.start).get(edgeB.start)[0]).add(stateMap.get(edgeA.end).get(edgeB.end)[0]);
        }

        if (b.finalStates.contains(edgeB.start)) {
          directlyReachable.get(stateMap.get(edgeA.start).get(edgeB.start)[1]).add(stateMap.get(edgeA.end).get(edgeB.end)[0]);
        } else {
          directlyReachable.get(stateMap.get(edgeA.start).get(edgeB.start)[1]).add(stateMap.get(edgeA.end).get(edgeB.end)[1]);
        }
      }
    }

    c.finalStates = new ArrayList<>();
    for (State stateA : a.states) {
      for (State finalB : b.finalStates) {
        c.finalStates.add(stateMap.get(stateA).get(finalB)[1]);
      }
    }

    for (State f : c.finalStates) {
      if (c.hasConnection(c.initialState, f, directlyReachable) && c.hasConnection(f, f,
              directlyReachable)) {
        return false;
      }
    }

    return true;
  }

  public static Automaton concat(Automaton a, Automaton b) {
    if (!a.alphabet.equals(b.alphabet)) {
      throw new IllegalArgumentException("Automatons to concat must have the same alphabet.");
    }

    Automaton c = new Automaton();

    c.alphabet = a.alphabet;

    c.states = new ArrayList<>();
    c.states.addAll(a.states);
    c.states.addAll(b.states);

    c.initialState = a.initialState;

    c.edges = new ArrayList<>();
    c.edges.addAll(a.edges);
    c.edges.addAll(b.edges);
    for (Edge edge : a.edges) {
      if (!a.finalStates.contains(edge.end)) continue;

      c.edges.add(new Edge(edge.start, b.initialState, edge.label));
    }

    if (a.finalStates.contains(a.initialState)) {
      for (Edge edge : b.edges) {
        if (!edge.start.equals(b.initialState)) continue;

        c.edges.add(new Edge(c.initialState, edge.end, edge.label));
      }
    }

    c.finalStates = new ArrayList<>();
    c.finalStates.addAll(b.finalStates);

    return c;
  }

  static Automaton omega(Automaton a) {
    Automaton b = new Automaton();

    b.alphabet = a.alphabet;

    b.states = new ArrayList<>();
    b.states.addAll(a.states);
    State init = new State(a.initialState.Name + ".2");
    b.states.add(init);

    b.initialState = init;

    b.edges = new ArrayList<>();
    b.edges.addAll(a.edges);
    for (Edge edge : a.edges) {
      if (edge.start == a.initialState) {
        b.edges.add(new Edge(init, edge.end, edge.label));
      }

      if (a.finalStates.contains(edge.end)) {
        b.edges.add(new Edge(edge.start, init, edge.label));

        if (edge.start == a.initialState) {
          b.edges.add(new Edge(init, init, edge.label));
        }
      }
    }

    b.finalStates = new ArrayList<>();
    b.finalStates.add(init);

    return b;
  }

  boolean acceptsWord(List<Object> word) {
    return finalStateReachable(word, 0, initialState);
  }

  private boolean finalStateReachable(List<Object> word, int index, State source) {
    if (index >= word.size()) {
      return finalStates.contains(source);
    }

    for (Edge edge : edges) {
      if (!edge.start.equals(source) || edge.label != word.get(index)) {
        continue;
      }

      boolean reachable = finalStateReachable(word, index + 1, edge.end);
      if (reachable) {
        return true;
      }
    }

    return false;
  }

  public boolean isBuechiEmpty() {
    Map<State, Set<State>> directlyReachable = new HashMap<>();

    for (State state : states) {
      directlyReachable.put(state, new HashSet<>());
    }

    for (Edge edge : edges) {
      directlyReachable.get(edge.start).add(edge.end);
    }


    for (State f : finalStates) {
      if (hasConnection(initialState, f, directlyReachable) && hasConnection(f, f,
              directlyReachable))
        return false;
    }

    return true;
  }

  private boolean hasConnection(State source, State sink,
                                Map<State, Set<State>> directlyReachable) {
    return hasConnection(source, sink, directlyReachable, new HashSet<>());
  }

  private boolean hasConnection(State source, State sink,
                                Map<State, Set<State>> directlyReachable, Set<State> visited) {
    if (directlyReachable.get(source).contains(sink)) {
      return true;
    }

    for (State reachable : directlyReachable.get(source)) {
      if (visited.contains(reachable)) continue;

      visited.add(reachable);
      boolean connected = hasConnection(reachable, sink, directlyReachable, visited);

      if (connected) return true;
    }

    return false;
  }

  private State getStateByName(String name) {
    for (State state : states) {
      if (state.Name.equals(name)) return state;
    }
    return null;
  }

  public void addState(String name) {
    states.add(new State(name));
  }

  public void addEdge(String stateName1, String stateName2, Object token) {
    // TODO: check for existence of states
    edges.add(new Edge(getStateByName(stateName1), getStateByName(stateName2), token));
  }

  public void addFinalState(String name) {
    finalStates.add(getStateByName(name));
  }

  public void setInitialState(String name) {
    initialState = getStateByName(name);
  }

  public void addAlphabetSymbol(Object token) {
    alphabet.add(token);
  }

  Map<Object, TransitionBox> computeAlphabetClasses() {
    Map<Object, TransitionBox> output = new HashMap<>();

    for (Object token : alphabet) {
      TransitionBox cls = TransitionBox.empty(states, Collections.singletonList(token));

      Map<State, Map<State, TransitionBox.Behaviour>> actions = cls.actions;

      for (Edge edge : edges) {
        if (!edge.label.equals(token)) continue;

        TransitionBox.Behaviour current = actions.get(edge.start).get(edge.end);
        if (finalStates.contains(edge.start) || finalStates.contains(edge.end))
          actions.get(edge.start).put(edge.end, TransitionBox.Behaviour.ReachableThroughFinal);
        else if (current == TransitionBox.Behaviour.NotReachable)
          actions.get(edge.start).put(edge.end, TransitionBox.Behaviour.Reachable);
      }

      output.put(token, cls);
    }

    return output;
  }

  SyntacticMonoid toMonoid() {
    Map<Object, TransitionBox> alphabetClasses = computeAlphabetClasses();

    List<TransitionBox> classes = new ArrayList<>();
    List<TransitionBox> classesToCheck = new ArrayList<>();
    List<TransitionBox> newClasses = new ArrayList<>();

    for (TransitionBox cls : alphabetClasses.values()) {
      if (!classes.contains(cls)) {
        classes.add(cls);
        classesToCheck.add(cls);
      }
    }

    boolean fixpoint;
    do {
      fixpoint = true;
      for (TransitionBox cls : classesToCheck) {
        for (Object token : alphabet) {
          TransitionBox newClass = TransitionBox.concat(cls, alphabetClasses.get(token));

          if (!classes.contains(newClass)) {
            classes.add(newClass);
            newClasses.add(newClass);
            fixpoint = false;
          }
        }
      }
      classesToCheck.clear();
      classesToCheck.addAll(newClasses);
      newClasses.clear();
    } while (!fixpoint);

    TransitionBox id = TransitionBox.neutral(states, finalStates);
    if (!classes.contains(id)) {
      classes.add(id);
    }
    return new SyntacticMonoid(classes, id);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("States: ");
    sb.append(states.toString());
    sb.append("\nInitial state: ");
    sb.append(initialState.toString());
    sb.append("\nFinal states: ");
    sb.append(finalStates.toString());
    sb.append("\nAlphabet: ");
    sb.append(alphabet.toString());
    sb.append("\nEdges: ");
    for (Edge e : edges) {
      sb.append("\t").append(e.toString()).append("\n");
    }

    return sb.toString();
  }

  private static class Edge {
    final State start;
    final State end;
    final Object label;

    Edge(State start, State end, Object label) {
      this.start = start;
      this.end = end;
      this.label = label;
    }

    public String toString() {
      return String.format("%s --> %s via %s", start.toString(), end.toString(), label);
    }
  }

  static class State {
    final String Name;

    State(String name) {
      this.Name = name;
    }

    @Override
    public String toString() {
      return this.Name;
    }
  }
}
