package cool.muyucloud.mnsimulator.models.protocols;

import cool.muyucloud.mnsimulator.data.Data;
import cool.muyucloud.mnsimulator.data.DSRPacket;
import cool.muyucloud.mnsimulator.models.layers.NetworkLayer;
import cool.muyucloud.mnsimulator.util.IPv4Address;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;

public class DSRProtocol extends NetworkLayerProtocol {
    private static final Class<DSRPacket> DEAL_TYPE = DSRPacket.class;

    public DSRProtocol(NetworkLayer layer) {
        super(layer);
    }

    @Override
    public void process(Data data) {
        if (!(data instanceof DSRPacket packet)) {
            throw new IllegalArgumentException(
                "DSRProtocol can only process %s data".formatted(DEAL_TYPE.getName()));
        }
        int tick = this.layer.station().media().getTick();
        IPv4Address address = this.layer.ip();
        switch (packet.type()) {
            case DSRPacket.TYPE_ROUTE_REQ -> {
                packet = packet.getForward(address);
                this.analyzeRREQ(packet, tick);
                this.layer.offer(packet.getRRep(address, this.layer.nextSyn()));
                if (!IPv4Address.equals(packet.target(), address)) {
                    this.layer.offer(packet);
                }
            }
            case DSRPacket.TYPE_ROUTE_REP -> {
                if (packet.path.contains(address)) {
                    analyzeRREP(packet);
                }
                if (IPv4Address.equals(address, packet.target())) { // If the station is the target, waste the packet
                    return;
                }
                this.layer.offer(packet);
            }
        }
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @Override
    public Class<? extends DSRPacket> processable() {
        return DEAL_TYPE;
    }

    private void analyzeRREP(@NotNull DSRPacket packet) {
        int hops = 0, tick = this.layer.station().media().getTick();
        IPv4Address next, address = this.layer.ip();
        NetworkLayer.Route route;
        Iterator<IPv4Address> ips = packet.path.iterator();
        // Goto ip of own
        while (ips.hasNext()) {
            IPv4Address ip = ips.next();
            if (IPv4Address.equals(ip, address)) {
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
        route = new NetworkLayer.Route(next, hops, NetworkLayer.Route.DIRECT, tick);
        this.layer.updateRoute(next, route);
        // Update DSR route
        while (ips.hasNext()) {
            ++hops;
            IPv4Address ip = ips.next();
            route = new NetworkLayer.Route(next, hops, NetworkLayer.Route.DSR, tick);
            this.layer.updateRoute(ip, route);
        }
    }

    private void analyzeRREQ(@NotNull DSRPacket packet, int tick) {
        int hops = packet.hops();
        IPv4Address next = packet.getBackward(this.layer.ip());
        // analyze route from RReq
        for (IPv4Address ip : packet.path) {
            if (IPv4Address.equals(ip, next)) {
                break;
            }
            NetworkLayer.Route route = new NetworkLayer.Route(next, hops, NetworkLayer.Route.DSR, tick);
            this.layer.updateRoute(ip, route);
            --hops;
        }
        NetworkLayer.Route route = new NetworkLayer.Route(next, 1, NetworkLayer.Route.DIRECT, tick);
        this.layer.updateRoute(next, route);
    }
}
