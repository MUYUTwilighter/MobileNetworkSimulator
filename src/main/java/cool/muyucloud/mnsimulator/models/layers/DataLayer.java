package cool.muyucloud.mnsimulator.models.layers;

import cool.muyucloud.mnsimulator.data.Data;
import cool.muyucloud.mnsimulator.data.Frame;
import cool.muyucloud.mnsimulator.data.PlainData;
import cool.muyucloud.mnsimulator.models.Station;
import cool.muyucloud.mnsimulator.util.MACAddress;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class DataLayer implements Layer {
    public static boolean RECORD = false;

    private static final int WAIT_OVERTIME = 20;    // Resend or cancel if overtime
    private static final int MAX_RESEND = 3;    // Including RTS resend and overall resend

    private static final byte STATE_AVAILABLE = 0x00;   // DataLayer is free to send
    private static final byte STATE_PREPARE = 0x01; // Preparing to send a frame
    private static final byte STATE_SEND_DATA = 0x02;   // Sending data
    private static final byte STATE_WAIT_ACK = 0x03;    // Waiting for ACK
    private static final byte STATE_SEND_ACK = 0x04;    // Sending ACK

    private final Station station;
    private final MACAddress address;
    private byte state = STATE_AVAILABLE;
    private int wait = 0;
    private int lastWait = 0;
    private int resend = 0;

    private final LinkedList<Frame> toSend = new LinkedList<>();
    private Frame sending = null;
    private final LinkedList<Frame> received = new LinkedList<>();

    public DataLayer(Station station) {
        this.station = station;
        this.address = MACAddress.createRandom();
    }

    @Override
    public void sendTick() {
        if (this.state == STATE_AVAILABLE) {
            this.initSend();
        }
        if (this.state == STATE_PREPARE) {
            this.trySend();
        }
        if (this.state == STATE_WAIT_ACK) {
            this.waitACK();
        }
    }

    /**
     * Layer is free and initialize a send-task
     */
    private void initSend() {
        if (this.toSend.isEmpty()) {
            return;
        }
        this.clearSend();
        this.sending = this.toSend.pollFirst();
        this.state = STATE_PREPARE;
    }

    /**
     * Layer is preparing a send-task, check channel and do sending
     */
    private void trySend() {
        // If not DIFS yet, return and continue timing
        if (this.wait > 0) {
            --this.wait;
            return;
        }
        // Reach DIFS, check channel
        if (this.station.physicalLayer().isAvailable()) {
            // Channel available, send
            this.send(this.sending);
        } else {
            // Channel busy, back to DIFS with doubled time
            this.wait = this.lastWait == 0 ? 1 : 2 * this.lastWait;
            this.lastWait = this.wait;
        }
    }

    /**
     * A data was sent, waiting for ACK
     */
    private void waitACK() {
        if (this.wait > 0) {    // Not overtime yet, do timing
            --this.wait;
            return;
        }
        if (this.resend > MAX_RESEND) { // Here mean a total failure, waste frame
            this.clearSend();
            return;
        }
        // A failed try, retry sending
        this.state = STATE_PREPARE;
        this.wait = WAIT_OVERTIME;
        ++this.resend;
    }

    @Override
    public void receiveTick() {
        Frame frame;
        while (!this.received.isEmpty()) {
            frame = this.received.pollFirst();
            switch (frame.type()) {
                case Frame.TYPE_DATA -> dealData(frame);
                case Frame.TYPE_ACK -> dealACK();
            }
        }
    }

    private void dealData(Frame frame) {
        Frame ack = new Frame(new PlainData(), this.address, frame.sender(), Frame.TYPE_ACK);
        this.toSend.addFirst(ack);
        this.station.networkLayer().receive(frame.data());
    }

    private void dealACK() {
        if (this.state == STATE_WAIT_ACK) {
            // Here means a frame was successfully sent
            this.clearSend();
        }
        // Here means the ACK might respond to an out-dated frame
    }

    @Override
    public void receive(Data data) {
        if (data instanceof Frame frame) {
            if (!this.addressMatch(frame.target())) {   // Not to me, ignore
                return;
            }
            if (this.addressMatch(frame.sender())) {    // Sent by myself, ignore
                return;
            }

            this.received.offer(frame);
            if (RECORD) {
                this.station.media().log(this.station, frame, "RECEIVE");
            }
        } else {
            this.station.networkLayer().receive(data);  // Not to this layer, forward
        }
    }

    @Override
    public void offer(Data data) {
        Frame frame = new Frame(data, this.address, MACAddress.BROADCAST, Frame.TYPE_DATA);
        this.toSend.offer(frame);
    }

    public void offer(Data data, @NotNull MACAddress target) {
        Frame frame = new Frame(data, this.address, target, Frame.TYPE_DATA);
        this.toSend.offer(frame);
    }

    protected void notifyFinish() {
        switch (this.state) {
            case STATE_SEND_DATA -> {
                // If is broadcast-frame or is not data-frame, no need to wait for ACK
                if (this.sending.isBroadcast() || this.sending.type() != Frame.TYPE_DATA) {
                    this.clearSend();
                    return;
                }
                // Finish sending data, wait ACK
                this.state = STATE_WAIT_ACK;
                this.wait = WAIT_OVERTIME;
            }
            case STATE_SEND_ACK -> this.clearSend();
        }
    }

    private boolean addressMatch(MACAddress address) {
        return MACAddress.equals(this.address, address)
            || MACAddress.equals(MACAddress.BROADCAST, address);
    }

    private void clearSend() {
        this.sending = null;
        this.wait = 0;
        this.lastWait = 0;
        this.resend = 0;
        this.state = STATE_AVAILABLE;
    }

    public MACAddress mac() {
        return this.address;
    }

    private void send(Frame frame) {
        this.station.physicalLayer().offer(frame);
        if (frame.type() == Frame.TYPE_DATA) {
            this.state = STATE_SEND_DATA;
        } else if (frame.type() == Frame.TYPE_ACK) {
            this.state = STATE_SEND_ACK;
        }
        if (RECORD) {
            this.station.media().log(this.station, frame, "SEND");
        }
    }
}
