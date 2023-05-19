package cool.muyucloud.mnsimulator.util;

public interface Address {
    String stringAddress();

    byte[] byteAddress();

    int len();

    static boolean equals(Address a, Address b) {
        return false;
    }
}
