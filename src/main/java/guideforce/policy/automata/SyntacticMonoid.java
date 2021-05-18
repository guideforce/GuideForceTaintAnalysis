package guideforce.policy.automata;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class SyntacticMonoid implements Iterable<TransitionBox> {
  private final int neutral;
  private final ArrayList<TransitionBox> elements;
  private final int[][] multiplicationTable;

  public SyntacticMonoid(List<TransitionBox> boxes, TransitionBox neutral) {
    this.elements = new ArrayList<>(boxes);
    this.neutral = this.elements.indexOf(neutral);
    int size = this.elements.size();

    this.multiplicationTable = new int[size][size];

    for (int x = 0; x < size; x++) {
      for (int y = 0; y < size; y++) {
        if (x == this.neutral) {
          this.multiplicationTable[x][y] = y;
          continue;
        }
        if (y == this.neutral) {
          this.multiplicationTable[x][y] = x;
          continue;
        }

        TransitionBox a = this.elements().get(x);
        TransitionBox b = this.elements().get(y);
        TransitionBox c = TransitionBox.concat(a, b);
        this.multiplicationTable[x][y] = boxes.indexOf(c);
      }
    }
  }

  public int neutral() {
    return neutral;
  }

  public ArrayList<TransitionBox> elements() {
    return elements;
  }

  public Integer multiply(Integer i, Integer j) {
    return multiplicationTable[i][j];
  }

  @Override
  @Nonnull
  public Iterator<TransitionBox> iterator() {
    return elements().iterator();
  }

}
