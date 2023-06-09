package cool.muyucloud.mnsimulator.models.layers;

import cool.muyucloud.mnsimulator.data.Data;
import cool.muyucloud.mnsimulator.data.Packet;
import cool.muyucloud.mnsimulator.models.Station;
import cool.muyucloud.mnsimulator.models.protocols.Behave;
import cool.muyucloud.mnsimulator.models.protocols.NetworkLayerProtocol;
import cool.muyucloud.mnsimulator.util.IPv4Address;
import cool.muyucloud.mnsimulator.util.MACAddress;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class NetworkLayer implements Layer {
    public static boolean RECORD = false;
    public static final int ROUTE_EXPIRE_TIME = 0x00001000;

    private final Station station;
    private final IPv4Address address;
    private final HashMap<IPv4Address, Route> routeTable = new HashMap<>();
    private final HashMap<Class<? extends Packet>, NetworkLayerProtocol> protocols = new HashMap<>();

    private final LinkedList<Packet> received = new LinkedList<>();
    private final LinkedList<Packet> toSend = new LinkedList<>();
    private final HashMap<IPv4Address, HashSet<Integer>> history = new HashMap<>();
    private final HashSet<Behave> onSendTick = new HashSet<>();

    private int syn = 0;

    public NetworkLayer(Station station, IPv4Address address) {
        this.station = station;
        this.address = address;
        // Add default route
        Route def = new Route(IPv4Address.BROADCAST, 0, Route.DEFAULT, 0x11111111);
        this.routeTable.put(IPv4Address.NULL, def);
    }

    public void regProtocol(NetworkLayerProtocol... protocols) {
        // Initialize additional protocols
        for (NetworkLayerProtocol protocol : protocols) {
            Class<? extends Packet> dataType = protocol.processable();
            NetworkLayerProtocol p = this.protocols.get(dataType);
            if (p != null) {
                throw new IllegalArgumentException(
                    "Corresponding %s processable protocol %s exists".formatted(dataType, p.getClass()));
            }
            this.protocols.put(dataType, protocol);
        }
    }

    public void regOnSendTick(Behave behave) {
        this.onSendTick.add(behave);
    }

    public void regOnStart(Behave behave) {
        behave.run();
    }

    @Override
    public void receive(Data data) {
        if (data instanceof Packet packet) {
            // filter packet send by myself
            if (IPv4Address.equals(packet.sender(), this.address)) {
                return;
            }
            // Filter dealt packet
            HashSet<Integer> synSet = this.history.computeIfAbsent(packet.sender(), k -> new HashSet<>());
            if (synSet.contains(packet.syn())) {
                return;
            }
            synSet.add(packet.syn());   // Record received
            this.received.offer(packet);    // Push to queue
            if (RECORD) {
                this.station.media().log(station, packet, "RECEIVE");
            }
        }
        this.station.receive(data);
    }

    @Override
    public void offer(Data data) {
        Packet packet = new Packet(data, this.address, IPv4Address.BROADCAST, Packet.TYPE_DATA, this.syn);
        ++this.syn;
        this.toSend.offer(packet);
    }

    public void offer(Data data, IPv4Address target) {
        if (IPv4Address.equals(target, this.address)) {
            this.station.receive(data);
            return;
        }
        Packet packet = new Packet(data, this.address, target, Packet.TYPE_DATA, this.syn);
        ++this.syn;
        this.toSend.offer(packet);
    }

    public void offer(Packet packet) {
        this.toSend.offer(packet);
    }

    public int nextSyn() {
        ++this.syn;
        return this.syn - 1;
    }

    public void sendTick() {
        for (Behave behave : onSendTick) {
            behave.run();
        }
        Packet packet;
        while (!this.toSend.isEmpty()) {
            packet = this.toSend.poll();
            this.send(packet);
        }
    }

    public void receiveTick() {
        Packet packet;
        while (!this.received.isEmpty()) {
            packet = this.received.poll();
            NetworkLayerProtocol protocol = this.protocols.get(packet.getClass());
            if (protocol == null) {
                continue;
            }
            protocol.process(packet);
        }
    }

    public void updateRoute(IPv4Address target, Route route) {
        // if first route
        if (!routeTable.containsKey(target) || IPv4Address.equals(target, IPv4Address.NULL)) {
            this.routeTable.put(target, route);
            return;
        }
        Route old = this.routeTable.get(target);
        int tick = this.station.media().getTick();
        // if old route out-dated
        if (tick - old.time > ROUTE_EXPIRE_TIME) {
            this.routeTable.put(target, route);
            return;
        }
        // if old route worse
        if (old.quality > route.quality()) {
            this.routeTable.put(target, route);
        }
    }

    private MACAddress findNext(IPv4Address target) {
        MACAddress next;
        if (this.routeTable.containsKey(target)) {
            next = this.station.media().arp(this.routeTable.get(target).next());
        } else {
            next = MACAddress.BROADCAST;
        }
        return next;
    }

    public IPv4Address ip() {
        return this.address;
    }

    // Pass task to lower layer
    private void send(Packet packet) {
        if (packet.isBroadcast()) {
            this.station.dataLayer().offer(packet);
        } else {
            MACAddress next = this.findNext(packet.target());
            this.station.dataLayer().offer(packet, next);
        }
        if (RECORD) {
            this.station.media().log(station, packet, "SEND");
        }
    }

    public record Route(IPv4Address next, int quality, byte type, int time) {
        public static final byte DEFAULT = 0x00;
        public static final byte DIRECT = 0x01;
        public static final byte DSR = 0x02;

        public Route(IPv4Address next, int quality, byte type, int time) {
            this.next = next;
            this.quality = quality;
            this.type = quality == 1 ? DIRECT : type;
            this.time = time;
        }

        private String stringType() {
            switch (this.type) {
                case DEFAULT -> {
                    return "DEFAULT";
                }
                case DIRECT -> {
                    return "DIRECT";
                }
                case DSR -> {
                    return "DSR";
                }
            }
            return "UNKNOWN";
        }

        @Override
        public String toString() {
            return "%s, %d, %s, %s".formatted(this.next, this.quality, this.stringType(), this.time);
        }
    }

    public HashMap<IPv4Address, Route> routeTable() {
        return this.routeTable;
    }

    public Station station() {
        return this.station;
    }
}
