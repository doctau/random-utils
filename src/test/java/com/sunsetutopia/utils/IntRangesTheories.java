package com.sunsetutopia.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.runner.RunWith;

import com.pholser.junit.quickcheck.ForAll;


@RunWith(Theories.class)
public class IntRangesTheories {

    @Test public void newSetIsEmpty() {
        IntRanges r = new IntRanges();
        assertTrue(r.isEmpty());
    }

    @Test public void emptySetZeroLength() {
        IntRanges r = new IntRanges();
        assertEquals(0, r.size());
    }

    @Theory public void clearedIsEmpty(@ForAll IntRanges r) {
        IntRanges old = r.clone();
        r.clear();
        assertTrue("r=" + old, r.isEmpty());
    }

    @Theory public void insertedMakesNotEmpty(@ForAll int n) {
        IntRanges r = new IntRanges();
        r.add(n);
        assertFalse("n=" + n, r.isEmpty());
    }

    @Theory public void insertingMakesPresent(@ForAll IntRanges r, @ForAll int n) {
        IntRanges old = r.clone();
        r.add(n);
        assertTrue("r=" + old + ", n=" + n, r.contains(n));
    }

    @Ignore
    @Theory public void removingMakesNotPresent(@ForAll IntRanges r, @ForAll int n) {
        IntRanges old = r.clone();
        r.remove(n);
        assertFalse("r=" + old + ", n=" + n, r.contains(n));
    }

    @Theory public void insertingSizeEqualLarger(@ForAll IntRanges r, @ForAll int n) {
        IntRanges old = r.clone();
        boolean alreadyPresent = r.contains(n);
        int oldSize = r.size();
        r.add(n);

        if (alreadyPresent)
            assertEquals("r=" + old + ", n=" + n, oldSize, r.size());
        else
            assertEquals("r=" + old + ", n=" + n, oldSize + 1, r.size());
    }

    @Ignore
    @Theory public void removingSizeEqualSmaller(@ForAll IntRanges r, @ForAll int n) {
        IntRanges old = r.clone();
        boolean alreadyPresent = r.contains(n);
        int oldSize = r.size();
        r.remove(n);

        if (alreadyPresent)
            assertEquals("r=" + old + ", n=" + n, oldSize - 1, r.size());
        else
            assertEquals("r=" + old + ", n=" + n, oldSize, r.size());
    }

    @Theory public void sizeEqualsIteratorCount(@ForAll IntRanges r) {
        int count = 0;
        Iterator<Integer> it = r.iterator();
        while (it.hasNext()) {
            it.next(); // ignore result
            count++;
        }
        assertEquals("r=" + r, r.size(), count);
    }

    @Theory public void unionContains(@ForAll IntRanges r1, @ForAll IntRanges r2) {
        IntRanges r3 = r1.clone();
        r3.addAll(r2);

        for (int i: r1) {
            assertTrue("r1=" + r1 + ", r2=" + r2, r3.contains(i));
        }

        for (int i: r2) {
            assertTrue("r1=" + r1 + ", r2=" + r2, r3.contains(i));
        }

        for (int i: r3) {
            assertTrue("r1=" + r1 + ", r2=" + r2, r1.contains(i) || r2.contains(i));
        }
    }

    @Theory public void selfEquality(@ForAll IntRanges r) {
        assertTrue("r=" + r, r.equals(r));
    }

    @Theory public void addAllEquality(@ForAll IntRanges r1) {
        IntRanges r2 = new IntRanges();
        r2.addAll(r1);

        assertEquals("r1=" + r1, r1, r2);
    }

    @Ignore
    @Theory public void intersectionContains(@ForAll IntRanges r1, @ForAll IntRanges r2) {
        IntRanges r3 = r1.clone();
        r3.retainAll(r2);

        for (int i: r3) {
            assertTrue("r1=" + r1 + ", r2=" + r2, r1.contains(i));
            assertTrue("r1=" + r1 + ", r2=" + r2, r2.contains(i));
        }

        for (int i: r1) {
            if (r2.contains(i))
                assertTrue("r1=" + r1 + ", r2=" + r2, r3.contains(i));
        }

        for (int i: r2) {
            if (r1.contains(i))
                assertTrue("r1=" + r1 + ", r2=" + r2, r3.contains(i));
        }
    }

    @Ignore
    @Theory public void differenceContains(@ForAll IntRanges r1, @ForAll IntRanges r2) {
        IntRanges r3 = r1.clone();
        r3.removeAll(r2);

        for (int i: r3) {
            assertTrue("r1=" + r1 + ", r2=" + r2, r1.contains(i));
            assertFalse("r1=" + r1 + ", r2=" + r2, r2.contains(i));
        }

        for (int i: r1) {
            assertTrue("r1=" + r1 + ", r2=" + r2, !r2.contains(i) == r3.contains(i));
        }
    }
}
