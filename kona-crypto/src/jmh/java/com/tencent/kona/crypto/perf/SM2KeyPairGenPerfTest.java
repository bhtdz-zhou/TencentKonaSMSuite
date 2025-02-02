package com.tencent.kona.crypto.perf;

import com.tencent.kona.crypto.TestUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.concurrent.TimeUnit;

import static com.tencent.kona.crypto.TestUtils.PROVIDER;

/**
 * The JMH-based performance test for SM2 key pair generation.
 */
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 5, time = 10)
@Fork(value = 2, jvmArgsAppend = {"-server", "-Xms2048M", "-Xmx2048M", "-XX:+UseG1GC"})
@Threads(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SM2KeyPairGenPerfTest {

    static {
        TestUtils.addProviders();
        Security.addProvider(new BouncyCastleProvider());
    }

    @State(Scope.Benchmark)
    public static class KeyPairGenHolder {

        KeyPairGenerator keyPairGenerator;

        @Setup
        public void setup() throws Exception {
            keyPairGenerator = KeyPairGenerator.getInstance("SM2", PROVIDER);
        }
    }

    @State(Scope.Benchmark)
    public static class KeyPairGenHolderBC {

        KeyPairGenerator keyPairGenerator;

        @Setup
        public void setup() throws Exception {
            keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(new ECGenParameterSpec("sm2p256v1"));
        }
    }

    @Benchmark
    public KeyPair genKeyPair(KeyPairGenHolder holder) {
        return holder.keyPairGenerator.generateKeyPair();
    }

    @Benchmark
    public KeyPair genKeyPairBC(KeyPairGenHolderBC holder) {
        return holder.keyPairGenerator.generateKeyPair();
    }
}
