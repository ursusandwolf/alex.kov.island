package com.island.bench;

import com.island.engine.core.HealthStorage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class SoABenchmark {

    private HealthStorage soaStore;
    private Map<Integer, Long> mapStore;
    private static final int ENTITY_COUNT = 10_000;

    @Setup
    public void setup() {
        soaStore = HealthStorage.create(ENTITY_COUNT);
        mapStore = new HashMap<>(ENTITY_COUNT);
        for (int i = 0; i < ENTITY_COUNT; i++) {
            soaStore.set(i, 1000L, 1000L, true);
            mapStore.put(i, 1000L);
        }
    }

    @Benchmark
    public void soaSequentialRead(Blackhole bh) {
        for (int i = 0; i < ENTITY_COUNT; i++) {
            bh.consume(soaStore.getCurrentEnergy(i));
        }
    }

    @Benchmark
    public void mapSequentialRead(Blackhole bh) {
        for (int i = 0; i < ENTITY_COUNT; i++) {
            bh.consume(mapStore.get(i));
        }
    }
}
