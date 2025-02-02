package com.tencent.kona.crypto.perf;

import com.tencent.kona.crypto.TestUtils;
import com.tencent.kona.crypto.util.Constants;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.Security;
import java.util.concurrent.TimeUnit;

import static com.tencent.kona.crypto.TestUtils.PROVIDER;
import static com.tencent.kona.crypto.CryptoUtils.toBytes;

/**
 * The JMH-based performance test for SM4 cipher.
 */
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 5, time = 10)
@Fork(value = 2, jvmArgsAppend = {"-server", "-Xms2048M", "-Xmx2048M", "-XX:+UseG1GC"})
@Threads(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SM4DecrypterPerfTest {

    private static final byte[] KEY = toBytes("0123456789abcdef0123456789abcdef");
    private static final byte[] IV = toBytes("00000000000000000000000000000000");
    private static final byte[] GCM_IV = toBytes("000000000000000000000000");

    private static final SecretKey SECRET_KEY = new SecretKeySpec(KEY, "SM4");
    private static final IvParameterSpec IV_PARAM_SPEC = new IvParameterSpec(IV);
    private static final GCMParameterSpec GCM_PARAM_SPEC
            = new GCMParameterSpec(Constants.SM4_GCM_TAG_LEN * 8, GCM_IV);

    private final static byte[] MESSAGE = TestUtils.dataMB(1);

    static {
        TestUtils.addProviders();
        Security.addProvider(new BouncyCastleProvider());
    }

    @State(Scope.Benchmark)
    public static class DecrypterHolder {

        byte[] ciphertextCBCPadding;
        byte[] ciphertextCBCNoPadding;
        byte[] ciphertextCTRNoPadding;
        byte[] ciphertextGCMNoPadding;

        Cipher decrypterCBCPadding;
        Cipher decrypterCBCNoPadding;
        Cipher decrypterCTRNoPadding;
        Cipher decrypterGCMNoPadding;

        @Setup(Level.Invocation)
        public void setup() throws Exception {
            setupCiphertexts();
            setupDecrypters();
        }

        private void setupCiphertexts() throws Exception {
            Cipher cipher = Cipher.getInstance("SM4/CBC/PKCS7Padding", PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, IV_PARAM_SPEC);
            ciphertextCBCPadding = cipher.doFinal(MESSAGE);

            cipher = Cipher.getInstance("SM4/CBC/NoPadding", PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, IV_PARAM_SPEC);
            ciphertextCBCNoPadding = cipher.doFinal(MESSAGE);

            cipher = Cipher.getInstance("SM4/CTR/NoPadding", PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, IV_PARAM_SPEC);
            ciphertextCTRNoPadding = cipher.doFinal(MESSAGE);

            cipher = Cipher.getInstance("SM4/GCM/NoPadding", PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, GCM_PARAM_SPEC);
            ciphertextGCMNoPadding = cipher.doFinal(MESSAGE);
        }

        private void setupDecrypters() throws Exception {
            decrypterCBCPadding = Cipher.getInstance(
                    "SM4/CBC/PKCS7Padding", PROVIDER);
            decrypterCBCPadding.init(
                    Cipher.DECRYPT_MODE, SECRET_KEY, IV_PARAM_SPEC);

            decrypterCBCNoPadding = Cipher.getInstance(
                    "SM4/CBC/NoPadding", PROVIDER);
            decrypterCBCNoPadding.init(
                    Cipher.DECRYPT_MODE, SECRET_KEY, IV_PARAM_SPEC);

            decrypterCTRNoPadding = Cipher.getInstance(
                    "SM4/CTR/NoPadding", PROVIDER);
            decrypterCTRNoPadding.init(
                    Cipher.DECRYPT_MODE, SECRET_KEY, IV_PARAM_SPEC);

            decrypterGCMNoPadding = Cipher.getInstance(
                    "SM4/GCM/NoPadding", PROVIDER);
            decrypterGCMNoPadding.init(
                    Cipher.DECRYPT_MODE, SECRET_KEY, GCM_PARAM_SPEC);
        }
    }

    @State(Scope.Benchmark)
    public static class DecrypterHolderBC {

        byte[] ciphertextCBCPadding;
        byte[] ciphertextCBCNoPadding;
        byte[] ciphertextCTRNoPadding;
        byte[] ciphertextGCMNoPadding;

        Cipher decrypterCBCPadding;
        Cipher decrypterCBCNoPadding;
        Cipher decrypterCTRNoPadding;
        Cipher decrypterGCMNoPadding;

        @Setup(Level.Invocation)
        public void setup() throws Exception {
            setupCiphertexts();
            setupDecrypters();
        }

        private void setupCiphertexts() throws Exception {
            Cipher cipher = Cipher.getInstance("SM4/CBC/PKCS7Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, IV_PARAM_SPEC);
            ciphertextCBCPadding = cipher.doFinal(MESSAGE);

            cipher = Cipher.getInstance("SM4/CBC/NoPadding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, IV_PARAM_SPEC);
            ciphertextCBCNoPadding = cipher.doFinal(MESSAGE);

            cipher = Cipher.getInstance("SM4/CTR/NoPadding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, IV_PARAM_SPEC);
            ciphertextCTRNoPadding = cipher.doFinal(MESSAGE);

            cipher = Cipher.getInstance("SM4/GCM/NoPadding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, GCM_PARAM_SPEC);
            ciphertextGCMNoPadding = cipher.doFinal(MESSAGE);
        }

        private void setupDecrypters() throws Exception {
            decrypterCBCPadding = Cipher.getInstance(
                    "SM4/CBC/PKCS7Padding", "BC");
            decrypterCBCPadding.init(
                    Cipher.DECRYPT_MODE, SECRET_KEY, IV_PARAM_SPEC);

            decrypterCBCNoPadding = Cipher.getInstance(
                    "SM4/CBC/NoPadding", "BC");
            decrypterCBCNoPadding.init(
                    Cipher.DECRYPT_MODE, SECRET_KEY, IV_PARAM_SPEC);

            decrypterCTRNoPadding = Cipher.getInstance(
                    "SM4/CTR/NoPadding", "BC");
            decrypterCTRNoPadding.init(
                    Cipher.DECRYPT_MODE, SECRET_KEY, IV_PARAM_SPEC);

            decrypterGCMNoPadding = Cipher.getInstance(
                    "SM4/GCM/NoPadding", "BC");
            decrypterGCMNoPadding.init(
                    Cipher.DECRYPT_MODE, SECRET_KEY, GCM_PARAM_SPEC);
        }
    }

    @Benchmark
    public byte[] cbcPadding(DecrypterHolder holder) throws Exception {
        return holder.decrypterCBCPadding.doFinal(holder.ciphertextCBCPadding);
    }

    @Benchmark
    public byte[] cbcPaddingBC(DecrypterHolderBC holder) throws Exception {
        return holder.decrypterCBCPadding.doFinal(holder.ciphertextCBCPadding);
    }

    @Benchmark
    public byte[] cbcNoPadding(DecrypterHolder holder) throws Exception {
        return holder.decrypterCBCNoPadding.doFinal(holder.ciphertextCBCNoPadding);
    }

    @Benchmark
    public byte[] cbcNoPaddingBC(DecrypterHolderBC holder) throws Exception {
        return holder.decrypterCBCNoPadding.doFinal(holder.ciphertextCBCNoPadding);
    }

    @Benchmark
    public byte[] ctr(DecrypterHolder holder) throws Exception {
        return holder.decrypterCTRNoPadding.doFinal(holder.ciphertextCTRNoPadding);
    }

    @Benchmark
    public byte[] ctrBC(DecrypterHolderBC holder) throws Exception {
        return holder.decrypterCTRNoPadding.doFinal(holder.ciphertextCTRNoPadding);
    }

    @Benchmark
    public byte[] gcm(DecrypterHolder holder) throws Exception {
        return holder.decrypterGCMNoPadding.doFinal(holder.ciphertextGCMNoPadding);
    }

    @Benchmark
    public byte[] gcmBC(DecrypterHolderBC holder) throws Exception {
        return holder.decrypterGCMNoPadding.doFinal(holder.ciphertextGCMNoPadding);
    }
}
