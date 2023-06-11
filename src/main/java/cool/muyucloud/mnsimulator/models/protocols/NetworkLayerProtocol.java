package cool.muyucloud.mnsimulator.models.protocols;

import cool.muyucloud.mnsimulator.data.Data;
import cool.muyucloud.mnsimulator.data.Packet;
import cool.muyucloud.mnsimulator.models.layers.Layer;
import cool.muyucloud.mnsimulator.models.layers.NetworkLayer;
import cool.muyucloud.mnsimulator.util.IPv4Address;

public class NetworkLayerProtocol implements Protocol {
    private static final Class<Packet> DEAL_TYPE = Packet.class;

    protected final NetworkLayer layer;

    public NetworkLayerProtocol(NetworkLayer layer) {
        this.layer = layer;
    }

    public static NetworkLayerProtocol create(Layer layer) {
        if (!(layer instanceof NetworkLayer networkLayer)) {
            throw new IllegalArgumentException("NetworkLayerProtocol can only initialize with network layer");
        }
        return new NetworkLayerProtocol(networkLayer);
    }

    @Override
    public void process(Data data) {
        if (!(data instanceof Packet packet)) {
            throw new IllegalArgumentException(
                "NetworkLayerProtocol only process %s".formatted(DEAL_TYPE));
        }
        IPv4Address address = layer.ip();
        if (IPv4Address.equals(address, packet.target())) {
            this.layer.station().receive(packet.data());
        } else {
            this.layer.offer(packet);
        }
    }

    @Override
    public Class<? extends Packet> processable() {
        return DEAL_TYPE;
    }

    @Override
    public boolean isBusy() {
        return false;
    }
}
