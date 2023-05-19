package cool.muyucloud.mnsimulator.data;

import cool.muyucloud.mnsimulator.util.MACAddress;
import org.jetbrains.annotations.NotNull;

public class Frame implements Data {
    public static final byte TYPE_DATA = 0x00;
    public static final byte TYPE_ACK = 0x01;

    private static final int HEAD_SIZE = 20;

    private final Data data;
    private final MACAddress sender;
    private final MACAddress target;
    private final byte type;

    public Frame(@NotNull Data data, MACAddress sender, MACAddress target, byte type) {
        this.data = data;
        this.sender = sender;
        this.target = target;
        this.type = type;
    }

    @Override
    public String toString() {
        return "[%s] %s -> %s"
            .formatted(this.innerType(), this.sender, this.target);
    }

    private String innerType() {
        switch (this.type) {
            case TYPE_DATA -> {
                return "DATA";
            }
            case TYPE_ACK -> {
                return "ACK";
            }
        }
        return "UNKNOWN";
    }

    @Override
    public int size() {
        return this.data.size() + HEAD_SIZE;
    }

    public @NotNull MACAddress sender() {
        return this.sender;
    }

    public @NotNull MACAddress target() {
        return this.target;
    }

    @Override
    public String desc() {
        return this.innerType();
    }

    @Override
    public String dataType() {
        return "Frame";
    }

    public byte type() {
        return this.type;
    }

    public Data data() {
        return this.data;
    }

    public boolean isBroadcast() {
        return MACAddress.equals(this.target, MACAddress.BROADCAST);
    }
}
