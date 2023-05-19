package cool.muyucloud.mnsimulator.util;

import cool.muyucloud.mnsimulator.MNSimulator;

import java.util.Arrays;
import java.util.Random;

public class MACAddress implements Address {
    public static final MACAddress BROADCAST = create(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF);

    private static final Random RANDOM = MNSimulator.RANDOM;

    private final byte[] address = {0, 0, 0, 0, 0, 0};

    public static MACAddress create(int... address) {
        MACAddress mac = new MACAddress();
        for (int i = 0; i < 6 && i < address.length; ++i) {
            mac.address[i] = (byte) address[i];
        }
        return mac;
    }

    public static MACAddress createRandom() {
        MACAddress mac = new MACAddress();
        for (int i = 0; i < 6; ++i) {
            mac.address[i] = (byte) (RANDOM.nextInt() % 256);
        }
        return mac;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; ++i) {
            String b = "%s:".formatted(Integer.toHexString(Byte.toUnsignedInt(this.address[i])));
            if (b.length() == 1) {
                builder.append(0);
            }
            builder.append(b);
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    @Override
    public String stringAddress() {
        return this.toString();
    }

    public byte[] byteAddress() {
        return Arrays.copyOf(address, 6);
    }

    @Override
    public int len() {
        return 6;
    }

    public static boolean equals(MACAddress a, MACAddress b) {
        for (int i = 0; i < 4; ++i) {
            if (a.address[i] != b.address[i]) {
                return false;
            }
        }
        return true;
    }
}
