package cool.muyucloud.mnsimulator.data;

import cool.muyucloud.mnsimulator.util.IPv4Address;
import org.jetbrains.annotations.NotNull;

public class Packet implements Data {
    public static final byte TYPE_DATA = 0x00;

    private static final int HEAD_SIZE = 20;

    private final Data content;
    private final IPv4Address sender;
    private final IPv4Address target;
    private final byte type;
    private final int syn;

    public Packet(Data data, IPv4Address sender, IPv4Address target, byte type, int syn) {
        if (sender == target) {
            throw new IllegalArgumentException("Cannot send to self");
        }

        this.content = data;
        this.sender = sender;
        this.target = target;
        this.type = type;
        this.syn = syn;
    }

    @Override
    public int size() {
        return this.content.size() + HEAD_SIZE;
    }

    @Override
    public @NotNull IPv4Address sender() {
        return sender;
    }

    @Override
    public @NotNull IPv4Address target() {
        return target;
    }

    @Override
    public String desc() {
        return "%s:%d".formatted(this.innerType(), this.syn);
    }

    public String innerType() {
        switch (this.type) {
            case TYPE_DATA -> {
                return "DATA";
            }
        }
        return "UNKNOWN";
    }

    @Override
    public String dataType() {
        return "Packet";
    }

    public byte type() {
        return this.type;
    }

    public int syn() {
        return this.syn;
    }

    @Override
    public Data data() {
        return this.content;
    }

    public boolean isBroadcast() {
        return IPv4Address.equals(this.target, IPv4Address.BROADCAST);
    }
}
