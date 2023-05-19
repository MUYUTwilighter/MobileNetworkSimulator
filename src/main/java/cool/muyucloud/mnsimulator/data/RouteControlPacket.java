package cool.muyucloud.mnsimulator.data;

import cool.muyucloud.mnsimulator.util.IPv4Address;

import java.util.Iterator;
import java.util.LinkedList;

public class RouteControlPacket extends Packet {
    public static final byte TYPE_ROUTE_REQ = 0x03;
    public static final byte TYPE_ROUTE_REP = 0x04;

    public final LinkedList<IPv4Address> path = new LinkedList<>();

    public RouteControlPacket(IPv4Address sender, IPv4Address target, byte type, int syn) {
        super(new PlainData(), sender, target, type, syn);
        if (type == TYPE_ROUTE_REQ) {
            this.path.offer(sender);
        }
    }

    // get a forward packet
    private RouteControlPacket(RouteControlPacket former, IPv4Address self) {
        super(former.data(), former.sender(), former.target(), TYPE_ROUTE_REQ, former.syn());
        for (IPv4Address address : former.path) {
            if (IPv4Address.equals(self, address)) {
                throw new IllegalArgumentException("Current station already exists in path");
            }
            this.path.offer(address);
        }
        this.path.offer(self);
    }

    public RouteControlPacket getForward(IPv4Address self) {
        if (this.type() != TYPE_ROUTE_REQ) {
            throw new IllegalStateException("Only route-request packet requires forward");
        }
        return new RouteControlPacket(this, self);
    }

    public IPv4Address getBackward(IPv4Address self) {
        IPv4Address backward = null;
        for (IPv4Address address : this.path) {
            if (IPv4Address.equals(address, self)) {
                return backward;
            }
            backward = address;
        }
        return null;
    }

    public int hops() {
        return this.path.size() - 1;
    }

    public RouteControlPacket getRRep(IPv4Address sender, int syn) {
        RouteControlPacket packet = new RouteControlPacket(sender, this.sender(), TYPE_ROUTE_REP, syn);
        for (IPv4Address ip : this.path) {
            packet.path.offer(ip);
        }
        return packet;
    }

    @Override
    public String innerType() {
        switch (this.type()) {
            case TYPE_ROUTE_REQ -> {
                return "ROUTE_REQ";
            }
            case TYPE_ROUTE_REP -> {
                return "ROUTE_REP";
            }
        }
        return super.innerType();
    }

    @Override
    public String desc() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.innerType()).append(":%d ".formatted(this.syn()));
        Iterator<IPv4Address> ips = this.path.iterator();
        while (ips.hasNext()) {
            IPv4Address ip = ips.next();
            builder.append(ip);
            if (ips.hasNext()) {
                builder.append(" -> ");
            }
        }
        return builder.toString();
    }

    @Override
    public String dataType() {
        return "RCPacket";
    }

    @Override
    public int size() {
        return super.size() + path.size() * 4;
    }
}
