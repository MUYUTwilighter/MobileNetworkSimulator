package cool.muyucloud.mnsimulator.util;

import cool.muyucloud.mnsimulator.MNSimulator;

import java.util.Arrays;
import java.util.Random;

public class IPv4Address implements Address {
    public static final IPv4Address BROADCAST = create(0, 255, 255, 255, 255);
    public static final IPv4Address NULL = create(0, 0, 0, 0, 0);

    private static final Random RANDOM = MNSimulator.RANDOM;

    private final byte[] address = {0, 0, 0, 0};
    private final byte[] cover = {0, 0, 0, 0};

    public static IPv4Address createRandom() {
        IPv4Address address = new IPv4Address();
        for (int i = 0; i < 4; ++i) {
            address.address[i] = (byte) (RANDOM.nextInt() % 256);
            address.cover[i] = (byte) 255;
        }
        return address;
    }

    public static IPv4Address createRandom(int coverLen, int... network) {
        IPv4Address address = createRandom();
        // generate cover
        for (int i = 0; i < coverLen; ++i) {
            address.cover[i / 8] |= (0b0_10000000 >>> (i % 8));
        }
        // remove random network
        for (int i = 0; i < 4; ++i) {
            address.address[i] &= ~address.cover[i];
        }
        // assign defined network
        for (int i = 0; i < network.length; ++i) {
            address.address[i] |= network[i] & address.cover[i];
        }
        return address;
    }

    public static IPv4Address create(int coverLen, int... ip) {
        IPv4Address address = new IPv4Address();
        for (int i = 0; i < ip.length; ++i) {
            address.address[i] = (byte) ip[i];
        }
        // generate cover
        for (int i = 0; i < coverLen; ++i) {
            address.cover[i / 8] |= (0b0_10000000 >>> (i % 8));
        }
        return address;
    }

    @Override
    public String toString() {
        return "%s/%d".formatted(this.stringAddress(), this.coverLen());
    }

    public String stringAddress() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; ++i) {
            String b = "%d.".formatted(Byte.toUnsignedInt(this.address[i]));
            builder.append(b);
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public String stringCover() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; ++i) {
            String b = "%d.".formatted(Byte.toUnsignedInt(this.cover[i]));
            builder.append(b);
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public String stringNetwork() {
        StringBuilder builder = new StringBuilder();
        byte[] network = this.byteNetwork();
        for (int i = 0; i < 4; ++i) {
            String b = "%d.".formatted(Byte.toUnsignedInt(network[i]));
            builder.append(b);
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public String stringHost() {
        StringBuilder builder = new StringBuilder();
        byte[] host = this.byteHost();
        for (int i = 0; i < 4; ++i) {
            String b = "%d.".formatted(Byte.toUnsignedInt(host[i]));
            builder.append(b);
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public byte[] byteAddress() {
        return Arrays.copyOf(this.address, 4);
    }

    @Override
    public int len() {
        return 4;
    }

    public byte[] byteCover() {
        return Arrays.copyOf(this.cover, 4);
    }

    public byte[] byteNetwork() {
        byte[] network = {0, 0, 0, 0};
        for (int i = 0; i < 4; ++i) {
            network[i] = (byte) (this.address[i] & this.cover[i]);
        }
        return network;
    }

    public byte[] byteHost() {
        byte[] host = {0, 0, 0, 0};
        for (int i = 0; i < 4; ++i) {
            host[i] = (byte) (~cover[i] & address[i]);
        }
        return host;
    }

    public static boolean equals(IPv4Address a, IPv4Address b) {
        for (int i = 0; i < 4; ++i) {
            if (a.address[i] != b.address[i]) {
                return false;
            }
        }
        return true;
    }

    public void changeCover(int coverLen) {
        for (int i = 0; i < coverLen; ++i) {
            this.cover[i / 8] |= (0b0_10000000 >>> (i % 8));
        }
    }

    public int coverLen() {
        int len = 0;
        for (byte b : this.cover) {
            int bInt = Byte.toUnsignedInt(b);
            if (bInt == 0x00) {
                return len;
            }
            for (int i = 0; i < 8; ++i) {
                if (b % 2 == 1) {
                    ++len;
                }
                b >>>= 1;
            }
        }
        return len;
    }
}
