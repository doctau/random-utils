package com.sunsetutopia.utils;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/*
 * Stores an int set using ranges.
 *
 * transitions indicates where the trailing edges of transitions are.
 * if MIN_VALUE is the first value, then it starts on.
 *
 * e.g. if transitions=[2,4,6] then
 * the set is {2, 3, 6...MAX_INT}
 */
public final class IntRanges extends AbstractSet<Integer> implements Cloneable {
    private final AtomicReference<int[]> _transitions;

    public IntRanges() {
        _transitions = new AtomicReference<int[]>();
        _transitions.set(new int[0]);
    }

    private static int lastIndexSmallerThan(int[] is, int n) {
        assert(is.length > 0);
        for (int i = 0; i < is.length; i++) {
            if (is[i] >= n)
                return i - 1;
        }
        return is.length;
    }

    private static void verifyTransitionTable(int[] is) {
        // must be in order, and no duplicates
        for (int i = 0; i < is.length - 1; i++) {
            assert(is[i] < is[i+1]);
        }
    }

    private int[] transitions() {
        return _transitions.get();
    }


    private boolean updateTransitions(final int[] old, final int[] newTransitions) {
        //verifyTransitionTable(newTransitions);
        return _transitions.compareAndSet(old, newTransitions);
    }


    private void updateTransitionsUnseen(final int[] newTransitions) {
        //verifyTransitionTable(newTransitions);
        _transitions.set(newTransitions);
    }


    public boolean contains(int n) {
        int[] transitions = transitions();
        if (transitions.length == 0) {
            return false;
        } else {
            int idx = lastIndexSmallerThan(transitions, n + 1);
            return (idx % 2 == 0) && (idx != transitions.length);
        }
    }

    public boolean add(int n) {
        while (true) {
            final int[] transitions = transitions();
            final int[] newTransitions;

            if (transitions.length == 0) {
                if (n != Integer.MAX_VALUE)
                    newTransitions = new int[] {n, n+1};
                else
                    newTransitions = new int[] {Integer.MAX_VALUE};
            } else {
                final int last = transitions[transitions.length - 1];
                final int idx = lastIndexSmallerThan(transitions, n);
                if (idx == -1) {
                    newTransitions = addAtStart(transitions, n);
                } else if (idx == transitions.length) {
                    newTransitions = addAtEnd(transitions, n, last, idx);
                } else {
                    // interior segment
                    newTransitions = addInterior(transitions, n, last, idx);
                }
            }

            if (transitions == newTransitions) {
                return false;
            } else {
                if (updateTransitions(transitions, newTransitions))
                    return true;
                // update failed, try again
            }
        }
    }

    private static int[] addAtStart(int[] transitions, int n) {
        if (transitions[0] == n) {
            // already there
            return transitions;
        } else if (transitions[0] == n + 1) {
            // prepend to initial segment
            int[] newTransitions = Arrays.copyOf(transitions, transitions.length);
            newTransitions[0] = n;
            return newTransitions;
        } else {
            // prepend new segment
            int[] newTransitions = new int[transitions.length + 2];
            System.arraycopy(transitions, 0, newTransitions, 2, transitions.length);
            newTransitions[0] = n;
            newTransitions[1] = n + 1;
            return newTransitions;
        }
    }

    private static int[] addAtEnd(int[] transitions, int n, final int last, final int idx) {
        if (idx % 2 == 1) {
            // goes to MAX_VALUE, already present
            return transitions;
        } else if (n == last + 1) {
            //append to final segment
            int[] newTransitions = Arrays.copyOf(transitions, transitions.length);
            newTransitions[transitions.length - 1] = n;
            return newTransitions;
        } else {
            // append new segment
            final int[] newTransitions;
            if (n == Integer.MAX_VALUE) {
                newTransitions = Arrays.copyOf(transitions, transitions.length + 1);
                newTransitions[transitions.length] = n;
            } else {
                newTransitions = Arrays.copyOf(transitions, transitions.length + 2);
                newTransitions[transitions.length + 1] = n + 1;
                newTransitions[transitions.length] = n;
            }
            return newTransitions;
        }
    }

    private static int[] addInterior(int[] transitions, int n, final int last, final int idx) {
        assert(idx >= 0);
        assert(idx < transitions.length);
        // assert not in a n...MAX_VALUE open segment either


        if (idx % 2 == 0) {
            // we're already in a segment, unless we are the first out
            if (n != transitions[idx + 1]) {
                // nothing to do, already in
                return transitions;
            } else if ((n + 1) == transitions[idx + 2]) {
                // need to merge segments
                // looks like [idx(...), idx+1(n), idx+2(n+1), ...]
                // drop idx+1 and idx+2
                int[] newTransitions = Arrays.copyOf(transitions, transitions.length - 2);
                System.arraycopy(transitions, 0, newTransitions, 0, idx + 1);
                System.arraycopy(transitions, idx+3, newTransitions, idx + 1, transitions.length - idx - 3);
                return newTransitions;
            } else {
                // just extend this one
                int[] newTransitions = Arrays.copyOf(transitions, transitions.length);
                newTransitions[idx + 1] = n + 1;
                return newTransitions;
            }
        } else {
            if (n+1 == transitions[idx+1]) {
                // extend next one back
                int[] newTransitions = Arrays.copyOf(transitions, transitions.length);
                newTransitions[idx + 1] = n;
                return newTransitions;
            } else {
                // add a new segment or merge
                // do add for now
                int[] newTransitions = new int[transitions.length + 2];
                System.arraycopy(transitions, 0, newTransitions, 0, idx + 1);
                System.arraycopy(transitions, idx + 1, newTransitions, idx + 3, transitions.length - idx - 1);
                newTransitions[idx + 1] = n;
                newTransitions[idx + 2] = n + 1;
                return newTransitions;
            }
        }
    }

    public boolean remove(int n) {
        throw new UnsupportedOperationException("TODO");
    }

    // basic methods

    public IntRanges clone() {
        IntRanges r = new IntRanges();
        r.updateTransitionsUnseen(transitions());
        return r;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        else if (o != null && o instanceof IntRanges) {
            return Arrays.equals(transitions(), ((IntRanges)o).transitions());
        } else {
            return false;
        }
    }

    public String toString() {
        int[] transitions = transitions();

        if (transitions.length == 0)
            return "{}";
        else if (transitions.length == 1)
            if (transitions[0] != Integer.MAX_VALUE)
                return "{[" + transitions[0] + ":" + Integer.MAX_VALUE + "]}";
            else
                return "{" + transitions[0] + "}";
        else if (transitions.length == 2)
            return "{" + transitions[0] + "}";
        else
            return segmentsToString(transitions);
    }

    // always more then one segment
    private static String segmentsToString(int[] transitions) {
        assert(transitions.length > 2);

        final int last = transitions.length - 1;
        StringBuilder sb = new StringBuilder("{");

        for (int i = 0; i < last; i += 2) {
            int start = transitions[i];
            int end = transitions[i+1] - 1;

            if (i != 0)
                sb.append(',');

            if (start == end) {
                sb.append(Integer.toString(start));
            } else {
                sb.append("[" + start + "-" + end + "]");
            }
        }

        // open ended
        if (transitions.length % 2 == 1) {
            if (transitions[last] == Integer.MAX_VALUE) {
                sb.append("," + transitions[last]);
            } else {
                sb.append(",[" + transitions[last] + "-" + Integer.MAX_VALUE + "]");
            }
        }

        sb.append("}");
        return sb.toString();
    }


    // Set methods

    public int size() {
        int[] transitions = transitions();

        if (transitions.length == 0)
            return 0;

        final int last = transitions.length - 1;
        int size = 0;
        for (int i = 0; i < last; i += 2) {
            int difference = transitions[i+1] - transitions[i];
            size += difference;
        }

        // open ended
        if (transitions.length % 2 == 1) {
            size += Integer.MAX_VALUE - transitions[last] + 1;
        }

        return size;
    }

    public boolean isEmpty() {
        return transitions().length == 0;
    }

    public void clear() {
        updateTransitionsUnseen(new int[0]);
    }

    public boolean contains(Object o) {
        return contains((int)((Integer) o));
    }

    public Iterator<Integer> iterator() {
        int[] transitions = transitions();

        if (transitions.length == 0) {
          Set<Integer> es = Collections.emptySet();
          return es.iterator();
        } else {
            return new IntRangeIterator(transitions[0], transitions, this);
        }
    }

    public boolean add(Integer o) {
        return add((int)((Integer) o));
    }

    public boolean remove(Object o) {
        return remove((int)((Integer) o));
    }

    /* could add for performance

    public boolean containsAll(Collection<?> c) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean addAll(Collection<? extends Integer> c) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean retainAll(Collection<?> c) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean removeAll(Collection<?> c) {
        // TODO Auto-generated method stub
        return false;
    }
    */


    private static class IntRangeIterator implements Iterator<Integer> {
        private final int[] transitions;
        private final IntRanges parent;
        private int next;
        private int nextTransition;

        public IntRangeIterator(int next, int[] transitions, IntRanges parent) {
            this.next = next;
            this.transitions = transitions;
            this.nextTransition = 1;
            this.parent = parent;
        }

        @Override
        public boolean hasNext() {
            return (nextTransition > 0) && nextTransition <= transitions.length;
        }

        /*
         * Example:
        (2,4,6,8) start n=2, nt=1
            hn()=true, next()=2, set n=3
            hn()=true, next()=3, set n=6, nt=3
            hn()=true, next()=6, set n=7
            hn()=true, next()=7, set n=8, nt=5
            hn()=false
        */

        @Override
        public Integer next() {
            if (parent.transitions() != transitions)
                throw new ConcurrentModificationException();
            if (!hasNext())
                throw new IllegalStateException();

            int n = next;

            if (next == Integer.MAX_VALUE) {
                // always end here
                nextTransition = -1;
            } else if (nextTransition >= transitions.length) {
                // stream to the end
                next = next + 1;
                //nextTransition = -1;
            } else if (next + 1 == transitions[nextTransition]) {
                // transitioning to off state
                if (nextTransition + 1 == transitions.length) {
                    // that it
                    nextTransition = -1;
                } else {
                    next = transitions[nextTransition+1];
                    nextTransition += 2;
                }
            } else {
                // move on
                next = next + 1;
            }
            return n;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
