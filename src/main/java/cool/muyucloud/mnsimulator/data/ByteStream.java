package cool.muyucloud.mnsimulator.data;

import cool.muyucloud.mnsimulator.util.PlainAddress;
import org.jetbrains.annotations.NotNull;

public class ByteStream implements Data {
    public static final byte TYPE_SENDING = 0x00;
    public static final byte TYPE_END = 0x01;
    public static final byte TYPE_INTERRUPT = 0x02;

    private final PlainAddress sender;
    private final PlainAddress target;
    private final Data data;
    private final byte type;

    public ByteStream(PlainAddress sender, PlainAddress target, Data data, byte type) {
        this.sender = sender;
        this.target = target;
        this.type = type;
        this.data = data;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Data data() {
        return this.data;
    }

    @Override
    public @NotNull PlainAddress sender() {
        return this.sender;
    }

    @Override
    public @NotNull PlainAddress target() {
        return this.target;
    }

    public String innerType() {
        switch (this.type) {
            case TYPE_SENDING -> {
                return "SENDING";
            }
            case TYPE_INTERRUPT -> {
                return "INTERRUPT";
            }
            case TYPE_END -> {
                return "END";
            }
        }
        return "UNKNOWN";
    }

    @Override
    public String desc() {
        return this.innerType();
    }

    @Override
    public String dataType() {
        return "ByteStream";
    }

    public byte type() {
        return this.type;
    }
}
