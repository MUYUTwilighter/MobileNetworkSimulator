package cool.muyucloud.mnsimulator.data;

import cool.muyucloud.mnsimulator.models.Station;
import cool.muyucloud.mnsimulator.util.IPv4Address;
import cool.muyucloud.mnsimulator.util.Pos;

public class LeachPacket extends Packet {
    public static final byte TYPE_HEAD_CONFIRM = 0x01;
    public static final byte TYPE_HELLO = 0x02;
    public static final byte TYPE_HEAD_CANCEL = 0x03;

    private final Pos pos;
    private final int sendTick;

    private LeachPacket(Data data, Station sender, byte type, int syn, int sendTick) {
        super(data, sender.networkLayer().ip(), IPv4Address.BROADCAST, type, syn);
        this.pos = sender.pos().copy();
        this.sendTick = sendTick;
    }

    public static LeachPacket createConfirm(Station sender, int syn, int sendTick) {
        return new LeachPacket(new PlainData(), sender, TYPE_HEAD_CONFIRM, syn, sendTick);
    }

    public static LeachPacket createHello(Station sender, int syn, int sendTick) {
        return new LeachPacket(new PlainData(), sender, TYPE_HELLO, syn, sendTick);
    }

    public static LeachPacket createCancel(Station sender, int syn, int sendTick) {
        return new LeachPacket(new PlainData(), sender, TYPE_HEAD_CANCEL, syn, sendTick);
    }

    public Pos pos() {
        return this.pos.copy();
    }

    @Override
    public String dataType() {
        return "LeachProtocol";
    }

    public String innerType() {
        switch (this.type()) {
            case TYPE_HEAD_CONFIRM -> {
                return "HEAD_CONFIRM";
            }
            case TYPE_HELLO -> {
                return "HELLO";
            }
            case TYPE_HEAD_CANCEL -> {
                return "HEAD_CANCEL";
            }
        }
        return super.innerType();
    }

    @Override
    public String desc() {
        return "[%s] %s:%d".formatted(innerType(), this.pos, this.sendTick);
    }

    public int sendTick() {
        return this.sendTick;
    }
}
