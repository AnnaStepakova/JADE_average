package average;

import java.io.Serializable;

public class Pair<T1 extends Serializable, T2 extends Serializable> implements Serializable {
    T1 first;
    T2 second;

    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return "(" + first.toString() + ", " + second.toString() + ")";
    }
}
