package cool.muyucloud.mnsimulator.util;

public class PlainAddress implements Address {
    private final String address;

    public PlainAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return this.address;
    }

    @Override
    public String stringAddress() {
        return this.address;
    }

    @Override
    public byte[] byteAddress() {
        return this.address.getBytes();
    }

    @Override
    public int len() {
        return address.length();
    }
}
