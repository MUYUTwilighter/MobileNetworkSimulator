package cool.muyucloud.mnsimulator.models.layers;

import cool.muyucloud.mnsimulator.data.ByteStream;
import cool.muyucloud.mnsimulator.data.Data;
import cool.muyucloud.mnsimulator.models.Media;
import cool.muyucloud.mnsimulator.models.Station;
import cool.muyucloud.mnsimulator.util.PlainAddress;

import java.util.HashSet;
import java.util.LinkedList;

public class PhysicalLayer implements Layer {
    public static boolean RECORD = false;

    private static final byte SEND_AVAILABLE = 0x00;    // Ready to send data
    private static final byte SEND_SENDING = 0x01;  // Currently sending data
    private static final byte SEND_INTERRUPTED = 0x02;  // Data-send cancelled
    private static final byte RECEIVE_AVAILABLE = 0x00; // No collision and not receiving
    private static final byte RECEIVE_BUSY = 0x01;  // Receiving data
    private static final byte RECEIVE_COLLISION = 0x02; // Collision detected

    private final Station station;

    private Data toSend = null;
    private int sent = 0;
    private byte sendState;

    private final LinkedList<ByteStream> received = new LinkedList<>();
    private final HashSet<Data> bad = new HashSet<>();
    private byte receiveState;

    public PhysicalLayer(Station station) {
        this.station = station;
        this.sendState = SEND_AVAILABLE;
        this.receiveState = SEND_AVAILABLE;
    }

    public void sendTick() {
        if (this.sendState == SEND_AVAILABLE) {
            return;
        }

        if (this.sendState == SEND_INTERRUPTED) {
            this.sendInterrupt();
            this.toSend = null;
            this.sent = 0;
            this.sendState = SEND_AVAILABLE;
            return;
        }

        if (this.sendState == SEND_SENDING) {
            this.radiate();
            if (this.sent > this.toSend.size()) {
                this.toSend = null;
                this.sent = 0;
                this.sendState = SEND_AVAILABLE;
                this.station.dataLayer().notifyFinish();
            }
        }
    }

    public void receiveTick() {
        ByteStream bytes;
        while (!this.received.isEmpty()) {
            bytes = this.received.pollFirst();
            if (bytes.type() == ByteStream.TYPE_END) {
                this.bad.remove(bytes.data());
                if (this.receiveState != RECEIVE_COLLISION) {
                    this.pass(bytes.data());
                }
            } else if (bytes.type() == ByteStream.TYPE_SENDING) {
                if (this.receiveState == RECEIVE_COLLISION) {
                    this.bad.add(bytes.data());
                }
            } else if (bytes.type() == ByteStream.TYPE_INTERRUPT) {
                this.bad.remove(bytes.data());
            }
        }
        if (this.bad.isEmpty()) {
            this.receiveState = RECEIVE_AVAILABLE;
        }
    }

    public void offer(Data data) {
        if (this.sendState != SEND_AVAILABLE) {
            return;
        }
        this.sendState = SEND_SENDING;
        this.toSend = data;
        this.sent = 0;
    }

    public void receive(Data data) {
        if (!(data instanceof ByteStream)) {
            throw new IllegalArgumentException("Only ByteStream can be dealt here");
        }
        if (RECORD) {
            this.station.media().log(station, data, "RECEIVE");
        }
        this.received.offer(((ByteStream) data));
        if (this.receiveState == RECEIVE_AVAILABLE) {
            this.receiveState = RECEIVE_BUSY;
        } else if (this.receiveState == RECEIVE_BUSY) {
            this.receiveState = RECEIVE_COLLISION;
        }
    }

    protected boolean isAvailable() {
        return this.sendState == SEND_AVAILABLE
            && this.receiveState == RECEIVE_AVAILABLE;
    }

    private void sendInterrupt() {
        if (this.toSend == null) {
            this.sent = 0;
            this.sendState = SEND_AVAILABLE;
        }
        Media media = this.station.media();
        byte type = ByteStream.TYPE_INTERRUPT;
        PlainAddress sender = new PlainAddress(this.station.getName());
        PlainAddress target = new PlainAddress("");
        ByteStream bytes = new ByteStream(sender, target, this.toSend, type);
        media.radiate(this.station, bytes);
    }

    private void radiate() {
        if (this.toSend == null) {
            this.sent = 0;
            this.sendState = SEND_AVAILABLE;
        }
        Media media = this.station.media();
        this.sent += media.speed();
        byte type = (this.sent > this.toSend.size()) ? ByteStream.TYPE_END : ByteStream.TYPE_SENDING;
        PlainAddress sender = new PlainAddress(this.station.getName());
        PlainAddress target = new PlainAddress("");
        ByteStream bytes = new ByteStream(sender, target, this.toSend, type);
        media.radiate(this.station, bytes);
        if (RECORD) {
            media.log(station, bytes, "send");
        }
    }

    private void pass(Data data) {
        this.station.dataLayer().receive(data);
    }
}
