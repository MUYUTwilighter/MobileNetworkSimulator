package cool.muyucloud.mnsimulator.models;

import cool.muyucloud.mnsimulator.data.Data;
import cool.muyucloud.mnsimulator.data.PlainData;
import cool.muyucloud.mnsimulator.models.layers.DataLayer;
import cool.muyucloud.mnsimulator.models.layers.NetworkLayer;
import cool.muyucloud.mnsimulator.models.layers.PhysicalLayer;
import cool.muyucloud.mnsimulator.util.IPv4Address;
import cool.muyucloud.mnsimulator.util.Pos;

public class Station {
    private final String name;
    private final Media media;
    private final int power;
    private final Pos pos;
    private final PhysicalLayer physicalLayer = new PhysicalLayer(this);
    private final DataLayer dataLayer = new DataLayer(this);
    private final NetworkLayer networkLayer;

    public Station(String name, Media media, int power, Pos pos, IPv4Address ip) {
        this.name = name;
        this.media = media;
        this.power = power;
        this.pos = pos;
        this.networkLayer = new NetworkLayer(this, ip);
    }

    public void sendTick() {
        this.networkLayer.sendTick();
        this.dataLayer.sendTick();
        this.physicalLayer.sendTick();
    }

    public void receiveTick() {
        this.physicalLayer.receiveTick();
        this.dataLayer.receiveTick();
        this.networkLayer.receiveTick();
    }

    public Media media() {
        return this.media;
    }

    public int power() {
        return this.power;
    }

    public Pos pos() {
        return this.pos;
    }

    public PhysicalLayer physicalLayer() {
        return physicalLayer;
    }

    public DataLayer dataLayer() {
        return dataLayer;
    }

    public NetworkLayer networkLayer() {
        return networkLayer;
    }

    public void receive(Data data) {

    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "%s, %d, %d, %s, %s".formatted(this.name, this.pos.x(), this.pos.y(), this.networkLayer.ip(), this.dataLayer.mac());
    }

    public void send(int size, IPv4Address target) {
        this.networkLayer.offer(new PlainData(size), target);
    }
}
