package guideforce.types;

import guideforce.policy.AbstractDomain;
import guideforce.policy.AbstractDomain.Finitary;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@Immutable
public final class Monad<A> {
  @Nonnull
  private final AbstractDomain domain;
  @Nonnull
  private final Map<A, Finitary> choices;

  public Monad(AbstractDomain domain, Map<A, Finitary> choices) {
    this.domain = domain;
    this.choices = choices;
  }

  public static <A> Monad<A> empty(AbstractDomain domain) {
    return new Monad<>(domain, Collections.emptyMap());
  }

  public static <A> Monad<A> pure(AbstractDomain domain, A value) {
    return new Monad<>(domain, Collections.singletonMap(value, domain.oneFinitary()));
  }

  public static <A> Monad<A> cases(AbstractDomain domain, Set<A> value) {
    HashMap<A, Finitary> result = new HashMap<>();
    for (A v : value) {
      result.put(v, domain.oneFinitary());
    }
    return new Monad<>(domain, result);
  }

  public <B> Monad<B> then(Function<A, Monad<B>> f) {
    HashMap<B, Finitary> result = new HashMap<>();
    for (Map.Entry<A,Finitary> choice : this.choices.entrySet()) {
      Monad<B> x = f.apply(choice.getKey());
      Finitary first = choice.getValue();
      for (Map.Entry<B,Finitary> y : x.choices.entrySet()) {
        B v = y.getKey();
        Finitary second = y.getValue();
        result.computeIfPresent(v, (v1, e) -> e.join(first.multiply(second)));
        result.putIfAbsent(v, first.multiply(second));
      }
    }
    return new Monad<>(domain, result);
  }

  public <B, C, D> Triple<Monad<B>, Monad<C>, Monad<D>> tripleThen(Function<A, Triple<Monad<B>, Monad<C>, Monad<D>>> f) {
    HashMap<B, Finitary> mb = new HashMap<>();
    HashMap<C, Finitary> mc = new HashMap<>();
    HashMap<D, Finitary> md = new HashMap<>();
    for (Map.Entry<A,Finitary> choice : this.choices.entrySet()) {
      Triple triple = f.apply(choice.getKey());
      Finitary ea = choice.getValue();
      if (triple.getFirst() != null) {
        for (Map.Entry<B,Finitary> x : ((Monad<B>) triple.getFirst()).choices.entrySet()) {
        B b = x.getKey();
        Finitary eb = ea.multiply(x.getValue());
        mb.computeIfPresent(b, (b1, e) -> e.join(eb));
        mb.putIfAbsent(b, eb);
        }
      }
      if (triple.getSecond() != null) {
        for (Map.Entry<C,Finitary> x : ((Monad<C>) triple.getSecond()).choices.entrySet()) {
          C c = x.getKey();
          Finitary ec = ea.multiply(x.getValue());
          mc.computeIfPresent(c, (c1, e) -> e.join(ec));
          mc.putIfAbsent(c, ec);
        }
      }
      if (triple.getThird() != null) {
        for (Map.Entry<D,Finitary> x : ((Monad<D>) triple.getThird()).choices.entrySet()) {
          D d = x.getKey();
          Finitary ed = ea.multiply(x.getValue());
          md.computeIfPresent(d, (d1, e) -> e.join(ed));
          md.putIfAbsent(d, ed);
        }
      }
    }
    return new Triple<>(new Monad<>(domain, mb), new Monad<>(domain, mc), new Monad<>(domain, md));
  }



  public Monad<A> then(Finitary effect) {
    HashMap<A, Finitary> result = new HashMap<>(this.choices.size());
    for (Map.Entry<A,Finitary> choice : this.choices.entrySet()) {
      A v = choice.getKey();
      Finitary e = choice.getValue();
      result.put(v, e.multiply(effect));
    }
    return new Monad<>(this.domain, result);
  }

  public <B> Monad<B> map(Function<A, B> f) {
    HashMap<B, Finitary> result = new HashMap<>(this.choices.size());
    for (Map.Entry<A,Finitary> choice : this.choices.entrySet()) {
      A v = choice.getKey();
      Finitary e = choice.getValue();
      result.put(f.apply(v), e);
    }
    return new Monad<>(this.domain, result);
  }

  public Monad<A> remove(A key) {
    HashMap<A, Finitary> result = new HashMap<>(choices);
    result.remove(key);
    return new Monad<>(domain, result);
  }

  public Finitary get(A key) {
    return choices.get(key);
  }

  @Nonnull
  public Map<A, Finitary> getChoices() {
    return choices;
  }

  public Monad<A> join(Monad<A> other) {
    HashMap<A, Finitary> merged = new HashMap<>(this.choices.size() + other.choices.size());
    for (Map.Entry<A,Finitary> entry : choices.entrySet()) {
      A v = entry.getKey();
      Finitary vType1 = entry.getValue();
      if (other.choices.containsKey(v)) {
        Finitary vType2 = other.choices.get(v);
        merged.put(v, vType1.join(vType2));
      } else {
        merged.put(v, vType1);
      }
    }
    for (Map.Entry<A,Finitary> entry : other.choices.entrySet()) {
      A v = entry.getKey();
      Finitary vType2 = entry.getValue();
      if (!choices.containsKey(v)) {
        merged.put(v, vType2);
      }
    }
    return new Monad<>(domain, merged);
  }

  public static <A> Monad<List<A>> sequence(AbstractDomain domain, List<Monad<A>> a) {
    int len = a.size();
    if (len == 0) {
      return Monad.pure(domain, Collections.emptyList());
    } else {
      Monad<A> t = a.get(len - 1);
      Monad<List<A>> s = sequence(domain, a.subList(0, len - 1));
      return s.then((List<A> l) ->
              t.then((A r) -> {
                List<A> nl = new LinkedList<>(l);
                nl.add(r);
                return Monad.pure(domain, nl);
              }));
    }
  }

  public Set<A> support() {
    return Collections.unmodifiableSet(choices.keySet());
  }

  public boolean isEmpty() {
    return choices.isEmpty();
  }

  public class Entry {
    private final A key;
    private final Finitary effect;

    public Entry(A key, Finitary effect) {
      this.key = key;
      this.effect = effect;
    }

    public Entry(Map.Entry<A, Finitary> entry) {
      this.key = entry.getKey();
      this.effect = entry.getValue();
    }

    public A getKey() {
      return key;
    }

    public Finitary getEffect() {
      return effect;
    }
  }

  public Stream<Entry> stream() {
    return choices.entrySet().stream().map(Entry::new);
  }

  public Finitary getAggregateFinitary() {
    return choices.values().stream().reduce(domain.zeroFinitary(), Finitary::join);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Monad<?> monad = (Monad<?>) o;
    return Objects.equals(choices, monad.choices);
  }

  @Override
  public int hashCode() {
    return Objects.hash(choices);
  }

  @Override
  public String toString() {
    if (choices.isEmpty()) {
      return "{}";
    }
    final StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<A,Finitary> e : choices.entrySet()) {
      sb.append(first ? "" : " | ");
      sb.append(e.getKey());
      sb.append(" & ").append(e.getValue());
      first = false;
    }
    return sb.toString();
  }
}
