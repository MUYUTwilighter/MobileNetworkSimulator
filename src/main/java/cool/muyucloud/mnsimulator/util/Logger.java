package cool.muyucloud.mnsimulator.util;

import cool.muyucloud.mnsimulator.models.Media;
import cool.muyucloud.mnsimulator.models.Station;
import cool.muyucloud.mnsimulator.models.layers.NetworkLayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

public class Logger {
    private static final Path STATIONS_REC = Path.of("stations.csv");
    private static final Path EVENT_REC = Path.of("events.csv");
    private static final Path TABLES_REC = Path.of("tables.csv");

    private final Media media;

    public Logger(Media media) {
        this.media = media;
    }

    public void dumpStations() throws IOException {
        HashSet<Station> stations = this.media.stations;
        if (!Files.exists(STATIONS_REC)) {
            Files.createFile(STATIONS_REC);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Name, posX, posY, IP, MAC\n");
        for (Station station : stations) {
            builder.append(station).append("\n");
        }
        Files.writeString(STATIONS_REC, builder.toString());
    }

    public void dumpEvents() throws IOException {
        if (!Files.exists(EVENT_REC)) {
            Files.createFile(EVENT_REC);
        }
        LinkedList<Event> events = this.media.events();
        StringBuilder builder = new StringBuilder();
        builder.append(Event.columns()).append("\n");
        for (Event event : events) {
            builder.append(event).append("\n");
        }
        Files.writeString(EVENT_REC, builder.toString());
    }

    public void dumpRouteTables() throws IOException  {
        if (!Files.exists(TABLES_REC)) {
            Files.createFile(TABLES_REC);
        }
        HashSet<Station> stations = this.media.stations;
        StringBuilder builder = new StringBuilder();
        builder.append("Station, target, next, quality, type, time").append("\n");
        for (Station station : stations) {
            HashMap<IPv4Address, NetworkLayer.Route> routeTable = station.networkLayer().routeTable();
            for (Map.Entry<IPv4Address, NetworkLayer.Route> entry : routeTable.entrySet()) {
                IPv4Address target = entry.getKey();
                NetworkLayer.Route route = entry.getValue();
                builder.append(station.getName()).append(", ");
                builder.append(target).append(", ");
                builder.append(route).append("\n");
            }
        }
        Files.writeString(TABLES_REC, builder.toString());
    }
}
