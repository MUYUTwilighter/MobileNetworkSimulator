package cool.muyucloud.mnsimulator.models.protocols;

import cool.muyucloud.mnsimulator.MNSimulator;
import cool.muyucloud.mnsimulator.data.Data;
import cool.muyucloud.mnsimulator.data.LeachPacket;
import cool.muyucloud.mnsimulator.models.Station;
import cool.muyucloud.mnsimulator.models.layers.NetworkLayer;
import cool.muyucloud.mnsimulator.util.IPv4Address;
import cool.muyucloud.mnsimulator.util.Pos;

import java.util.HashMap;
import java.util.Random;

public class LeachProtocol extends NetworkLayerProtocol {
    public static final float DEFAULT_HEAD_RATIO = 0.1f;
    public static final int HEAD_EXPIRE = 10000;
    public static final int WAIT_VOTE = 100;

    private static final Random RANDOM = MNSimulator.RANDOM;
    private static final Class<LeachPacket> DEAL_TYPE = LeachPacket.class;
    private boolean isHead;
    private int round;
    private int lastHead;
    private int nextVote = 0;
    private int randWait = 0;
    private IPv4Address myHead = IPv4Address.BROADCAST;
    private final HashMap<IPv4Address, Pos> detected = new HashMap<>();
    private final HashMap<IPv4Address, Integer> heads = new HashMap<>();

    public LeachProtocol(NetworkLayer layer) {
        super(layer);
        this.isHead = false;
        this.round = 0;
        this.lastHead = 0;
        this.detected.put(layer.ip(), layer.station().pos());
        this.nextVote = HEAD_EXPIRE;

        layer.regOnSendTick(args -> onSendTick());
        layer.regOnStart(args -> onStart());
    }

    @Override
    public void process(Data data) {
        if (!(data instanceof LeachPacket packet)) {
            throw new IllegalArgumentException(
                "Leach Protocol can only process %s data".formatted(DEAL_TYPE.getName()));
        }
        int tick = this.layer.station().media().getTick();
        this.detected.put(packet.sender(), packet.pos());
        switch (packet.type()) {
            case LeachPacket.TYPE_HEAD_CONFIRM -> {
                this.heads.put(packet.sender(), tick);
                this.calcGroup();
            }
            case LeachPacket.TYPE_HEAD_CANCEL -> {
                this.heads.remove(packet.sender());
                this.calcGroup();
            }
        }
        if (nextVote < tick) {
            nextVote = tick;
        }
        nextVote += WAIT_VOTE;
    }

    public void calcGroup() {
        int minDis = Integer.MAX_VALUE;
        this.myHead = IPv4Address.BROADCAST;
        this.updateRoute();
        for (IPv4Address head : this.heads.keySet()) {
            Pos headPos = this.detected.get(head);
            int headDis = this.layer.station().pos().distanceTo(headPos);
            if (headDis < minDis) {
                this.myHead = head;
                this.updateRoute();
            }
        }
    }

    private void updateRoute() {
        if (this.myHead == null) {
            return;
        }
        int tick = this.layer.station().media().getTick();
        NetworkLayer.Route route = new NetworkLayer.Route(
            this.myHead, 0, NetworkLayer.Route.DEFAULT, tick + HEAD_EXPIRE);
        this.layer.updateRoute(IPv4Address.NULL, route);
    }

    private void onSendTick() {
        int tick = this.layer.station().media().getTick();
        if (nextVote > tick) {
            return;
        }
        if (nextVote == tick) {
            this.randWait = RANDOM.nextInt(0, WAIT_VOTE);
        }
        if (randWait > 0) {
            randWait--;
            return;
        }

        // If no more leach packet received and head expired
        int head = this.heads.size(), total = this.detected.size();
        float p = (float) (head == 0 ? 1 : head) / total;
        if (lastHead != 0 && (round - lastHead) < (1 / p)) {
            return;
        }
        float weight = p / (1 - p * (round % (1 / p)));
        float random = RANDOM.nextFloat(0.0f, 1.0f);
        if (weight >= random) {
            this.isHead = true;
            this.lastHead = this.round;
            this.announceHead();
        } else if (this.isHead) {
            this.isHead = false;
            this.announceCancel();
        }
        this.round++;
        if (nextVote < tick) {
            nextVote = tick;
        }
        nextVote += HEAD_EXPIRE;
    }

    private void onStart() {
        Station station = this.layer.station();
        int tick = this.layer.station().media().getTick();
        LeachPacket packet = LeachPacket.createHello(station, layer.nextSyn(), tick);
        layer.offer(packet);
    }

    @Override
    public Class<? extends LeachPacket> processable() {
        return DEAL_TYPE;
    }

    private void announceHead() {
        int tick = this.layer.station().media().getTick();
        int syn = this.layer.nextSyn();
        Station station = this.layer.station();
        LeachPacket packet = LeachPacket.createConfirm(station, syn, tick);
        this.layer.offer(packet);
    }

    private void announceCancel() {
        int tick = this.layer.station().media().getTick();
        int syn = this.layer.nextSyn();
        Station station = this.layer.station();
        LeachPacket packet = LeachPacket.createCancel(station, syn, tick);
        this.layer.offer(packet);
    }
}
