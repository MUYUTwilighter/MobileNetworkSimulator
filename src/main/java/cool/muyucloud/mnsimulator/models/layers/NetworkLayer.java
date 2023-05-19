package cool.muyucloud.mnsimulator.models.layers;

import cool.muyucloud.mnsimulator.data.Data;
import cool.muyucloud.mnsimulator.data.Packet;
import cool.muyucloud.mnsimulator.data.RouteControlPacket;
import cool.muyucloud.mnsimulator.models.Station;
import cool.muyucloud.mnsimulator.util.IPv4Address;
import cool.muyucloud.mnsimulator.util.MACAddress;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public class NetworkLayer implements Layer {
    public static boolean RECORD = false;
    public static final int ROUTE_EXPIRE_TIME = 0x00001000;

    private final Station station;
    private final IPv4Address address;
    private final HashMap<IPv4Address, Route> routeTable = new HashMap<>();

    private final LinkedList<Packet> received = new LinkedList<>();
    private final LinkedList<Packet> toSend = new LinkedList<>();
    private final HashMap<IPv4Address, HashSet<Integer>> history = new HashMap<>();

    private int syn = 0;

    public NetworkLayer(Station station, IPv4Address address) {
        this.station = station;
        this.address = address;
        RouteControlPacket hello = new RouteControlPacket(this.address, IPv4Address.BROADCAST, RouteControlPacket.TYPE_ROUTE_REQ, this.syn);
        ++this.syn;
        this.toSend.offer(hello);
        Route def = new Route(IPv4Address.BROADCAST, 0, Route.DEFAULT, 0x11111111);
        this.routeTable.put(IPv4Address.NULL, def);
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
            synSet.add(packet.syn());
            this.received.offer(packet);
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

    public void sendTick() {
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
            if (packet instanceof RouteControlPacket rcPacket) {
                this.dealRCPacket(rcPacket);
            } else {
                this.dealDataPacket(packet);
            }
        }
    }

    private void dealRCPacket(@NotNull RouteControlPacket packet) {
        int tick = this.station.media().getTick();
        switch (packet.type()) {
            case RouteControlPacket.TYPE_ROUTE_REQ -> {
                packet = packet.getForward(this.address);
                analyzeRREQ(packet, tick);
                // if req is for us, send RouteRep
                if (this.addressMatch(packet.target())) {
                    this.toSend.offer(packet.getRRep(this.address, this.syn));
                    ++this.syn;
                    if (packet.isBroadcast()) {
                        this.toSend.offer(packet);
                    }
                } else {    // if not, forward
                    this.toSend.offer(packet);
                }
            }
            case RouteControlPacket.TYPE_ROUTE_REP -> {
                if (packet.path.contains(this.address)) {
                    analyzeRREP(packet, tick);
                }
                if (IPv4Address.equals(this.address, packet.target())) {
                    return;
                }
                this.toSend.offer(packet);
            }
        }
    }

    private void analyzeRREP(@NotNull RouteControlPacket packet, int tick) {
        int hops = 0;
        IPv4Address next;
        Route route;
        Iterator<IPv4Address> ips = packet.path.iterator();
        // Goto ip of own
        while (ips.hasNext()) {
            IPv4Address ip = ips.next();
            if (IPv4Address.equals(ip, this.address)) {
                break;
            }
        }
        // if not further route, exit
        if (!ips.hasNext()) {
            return;
        }
        // Update direct route
        ++hops;
        next = ips.next();
        route = new Route(next, hops, Route.DIRECT, tick);
        this.updateRoute(next, route);
        // Update DSR route
        while (ips.hasNext()) {
            ++hops;
            IPv4Address ip = ips.next();
            route = new Route(next, hops, Route.DSR, tick);
            this.updateRoute(ip, route);
        }
    }

    private void analyzeRREQ(@NotNull RouteControlPacket packet, int tick) {
        int hops = packet.hops();
        IPv4Address next = packet.getBackward(this.address);
        // analyze route from RReq
        for (IPv4Address ip : packet.path) {
            if (IPv4Address.equals(ip, next)) {
                break;
            }
            Route route = new Route(next, hops, Route.DSR, tick);
            this.updateRoute(ip, route);
            --hops;
        }
        Route route = new Route(next, 1, Route.DIRECT, tick);
        this.updateRoute(next, route);
    }

    private void updateRoute(IPv4Address target, Route route) {
        // if first route
        if (!routeTable.containsKey(target)) {
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

    private void dealDataPacket(@NotNull Packet packet) {
        if (IPv4Address.equals(this.address, packet.target())) {
            this.station.receive(packet.data());
        } else {
            this.send(packet);
        }
    }

    private MACAddress findNext(IPv4Address target) {
        MACAddress next;
        if (this.routeTable.containsKey(target)) {
            next = this.station.media().arp(this.routeTable.get(target).next());
        } else {
            this.sendRReq(target);
            next = MACAddress.BROADCAST;
        }
        return next;
    }

    private void sendRReq(IPv4Address target) {
        if (IPv4Address.equals(target, IPv4Address.BROADCAST)) {
            throw new IllegalArgumentException();
        }
        RouteControlPacket packet = new RouteControlPacket(
            this.address, target, RouteControlPacket.TYPE_ROUTE_REQ, this.syn);
        ++this.syn;
        this.toSend.offer(packet);
    }

    private boolean addressMatch(IPv4Address address) {
        return IPv4Address.equals(address, this.address)
            || IPv4Address.equals(address, IPv4Address.BROADCAST);
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
}
