package com.sunsetutopia.utils;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.java.lang.IntegerGenerator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.sunsetutopia.utils.IntRanges;

public class IntRangesGenerator extends Generator<IntRanges> {
    private final IntegerGenerator intGen;
    public IntRangesGenerator() {
        super(IntRanges.class);
        intGen = new IntegerGenerator();
    }

    @Override
    public IntRanges generate(SourceOfRandomness random, GenerationStatus status) {
        IntRanges r = new IntRanges();

        for (int i = 0; i < status.size(); ++i)
            r.add(intGen.generate(random, status));

        assert(r.size() == status.size());

        return r;
    }
}
