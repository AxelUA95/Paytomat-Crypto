package com.paytomat.bip44generator;

import com.paytomat.core.Constants;
import com.paytomat.core.util.ByteSerializer;
import com.paytomat.core.util.HashUtil;

import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * created by Alex Ivanov on 2019-02-12.
 */
public class HDNode {

    private static final int HARDENED_MARKER = 0x80000000;
    private static final String BITCOIN_SEED = "Bitcoin seed";
    private static final int CHAIN_CODE_SIZE = 32;

    public static HDNode fromSeed(byte[] seedBytes) throws GeneratorException {
        byte[] keyBytes;
        keyBytes = BITCOIN_SEED.getBytes(StandardCharsets.US_ASCII);
        byte[] I = HashUtil.hmacSha512(keyBytes, seedBytes);

        // Construct private key
        byte[] IL = Arrays.copyOf(I, 32);
        BigInteger k = new BigInteger(1, IL);
        if (k.compareTo(Constants.SECP256k1_PARAMS.getN()) >= 0) {
            throw new GeneratorException("An unlikely thing happened: The derived key is larger than the N modulus of the curve");
        }
        if (k.equals(BigInteger.ZERO)) {
            throw new GeneratorException("An unlikely thing happened: The derived key is zero");
        }
        // Construct chain code
        byte[] IR = Arrays.copyOfRange(I, 32, 32 + CHAIN_CODE_SIZE);
        return new HDNode(IL, IR);
    }

    private final byte[] privateKey;
    private byte[] publicKey;
    private final byte[] chainCode;
    private final HDPath path;
    private final int parentFingerprint;

    private HDNode(byte[] privateKey, byte[] chainCode) {
        this(privateKey, chainCode, HDPath.ROOT, 0);
    }

    private HDNode(byte[] privateKey, byte[] chainCode, HDPath path, int parentFingerprint) {
        if (privateKey.length != 32) throw new GeneratorException("Wrong privateKey Size");
        this.privateKey = privateKey;
        this.chainCode = chainCode;
        this.path = path;
        this.parentFingerprint = parentFingerprint;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public HDNode createChildNode(HDPath keyPath) {
        List<Integer> addrN = keyPath.getAddressN();
        HDNode node = this;
        for (Integer i : addrN) {
            node = node.createChildNode(i);
        }
        return node;
    }

    public HDNode createChildNode(int index) throws GeneratorException {
        boolean isHardened = (index & HARDENED_MARKER) != 0;
        ByteSerializer serializer = ByteSerializer.create();
        if (isHardened) {
            serializer.write((byte) 0)
                    .write(privateKey)
                    .writeBE(index);
        } else {
            serializer.write(getPublicKey())
                    .writeBE(index);
        }
        byte[] data = serializer.serialize();

        byte[] l = HashUtil.hmacSha512(chainCode, data);
        byte[] lL = Arrays.copyOfRange(l, 0, 32);
        byte[] lR = Arrays.copyOfRange(l, 32, 64);

        BigInteger m = new BigInteger(1, lL);
        if (m.compareTo(Constants.SECP256k1_PARAMS.getN()) >= 0) {
            throw new GeneratorException(
                    "An unlikely thing happened: A key derivation parameter is larger than the N modulus of the curve");
        }

        BigInteger kPar = new BigInteger(1, privateKey);
        BigInteger k = m.add(kPar).mod(Constants.SECP256k1_PARAMS.getN());
        if (k.equals(BigInteger.ZERO)) {
            throw new GeneratorException("An unlikely thing happened: The derived key is zero");
        }

        // Make a 32 byte result where k is copied to the end
        byte[] privateKeyBytes = bigIntegerTo32Bytes(k);
        return new HDNode(privateKeyBytes, lR, path.getChild(index & ~HARDENED_MARKER, isHardened), getFingerprint());
    }

    private byte[] bigIntegerTo32Bytes(BigInteger b) {
        // Returns an array of bytes which is at most 33 bytes long, and possibly
        // with a leading zero
        byte[] bytes = b.toByteArray();
        if (bytes.length == 33) {
            // The result is 32 bytes, but with zero at the beginning, which we
            // strip
            return Arrays.copyOfRange(bytes, 1, 33);
        }
        // The result is 32 bytes or less, make it 32 bytes with the data at the
        // end
        byte[] result = new byte[32];
        System.arraycopy(bytes, 0, result, result.length - bytes.length, bytes.length);
        return result;
    }

    private BigInteger getPrivateKeyBigInt() {
        byte[] keyBytes = new byte[33];
        System.arraycopy(privateKey, 0, keyBytes, 1, 32);
        return new BigInteger(keyBytes);
    }

    public byte[] getPublicKey() {
        if (publicKey == null) {
            ECPoint Q = Constants.SECP256k1_CURVE.getG().multiply(getPrivateKeyBigInt());
            // Convert Q to a compressed point on the curve
            publicKey = Q.getEncoded(true);
        }
        return publicKey;
    }

    private byte[] getIdentifier() {
        return HashUtil.sha256ripemd160(getPublicKey());
    }

    private int getFingerprint() {
        return ByteBuffer.wrap(Arrays.copyOfRange(getIdentifier(), 0, 4)).getInt();
    }

    int getParentFingerprint() {
        return parentFingerprint;
    }

    byte getDepth() {
        return path.getDepth();
    }

    int getIndex() {
        return path.getValue();
    }

    byte[] getChainCode() {
        return chainCode;
    }

    byte[] getPrivateKey33() {
        byte[] priv33 = new byte[33];
        byte[] priv = getPrivateKey();
        System.arraycopy(priv, 0, priv33, 33 - priv.length, priv.length);
        return priv33;
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
