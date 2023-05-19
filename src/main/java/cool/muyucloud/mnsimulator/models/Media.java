package cool.muyucloud.mnsimulator.models;

import cool.muyucloud.mnsimulator.data.ByteStream;
import cool.muyucloud.mnsimulator.data.Data;
import cool.muyucloud.mnsimulator.util.Event;
import cool.muyucloud.mnsimulator.util.IPv4Address;
import cool.muyucloud.mnsimulator.util.MACAddress;
import cool.muyucloud.mnsimulator.util.Pos;

import java.util.HashSet;
import java.util.LinkedList;

public class Media {
    private int tick;
    private final int speed;
    public final HashSet<Station> stations = new HashSet<>();

    private final LinkedList<Event> events = new LinkedList<>();

    public Media(int speed) {
        this.speed = speed;
    }

    public void addStation(String name, Pos pos, int power, IPv4Address ip) {
        Station station = new Station(name, this, power, pos, ip);
        stations.add(station);
    }

    public void radiate(Station sender, ByteStream bytes) {
        for (Station station : this.stations) {
            if (station.pos().distanceTo(sender.pos()) > sender.power()) {
                continue;
            }
            station.physicalLayer().receive(bytes);
        }
    }

    public void tick() {
        this.sendTick();
        this.receiveTick();
        ++this.tick;
    }

    private void sendTick() {
        for (Station station : this.stations) {
            station.sendTick();
        }
    }

    private void receiveTick() {
        for (Station station : this.stations) {
            station.receiveTick();
        }
    }

    public int speed() {
        return speed;
    }

    public int getTick() {
        return this.tick;
    }

    public MACAddress arp(IPv4Address address) {
        for (Station station : this.stations) {
            if (IPv4Address.equals(station.networkLayer().ip(), address)) {
                return station.dataLayer().mac();
            }
        }
        return MACAddress.BROADCAST;
    }

    public void log(Station station, Data data, String state) {
        this.events.offer(Event.log(this.tick, station, state, data));
    }

    public LinkedList<Event> events() {
        return this.events;
    }
}
