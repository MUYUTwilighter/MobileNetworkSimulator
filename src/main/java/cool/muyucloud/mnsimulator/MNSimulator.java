package cool.muyucloud.mnsimulator;

import cool.muyucloud.mnsimulator.models.Media;
import cool.muyucloud.mnsimulator.models.layers.DataLayer;
import cool.muyucloud.mnsimulator.models.layers.NetworkLayer;
import cool.muyucloud.mnsimulator.models.layers.PhysicalLayer;
import cool.muyucloud.mnsimulator.util.IPv4Address;
import cool.muyucloud.mnsimulator.util.Logger;
import cool.muyucloud.mnsimulator.util.Pos;

import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.Random;

public class MNSimulator {
    public static final long START_TIME = LocalTime.now().getLong(ChronoField.MILLI_OF_SECOND);
    public static final Random RANDOM = new Random(0);
    private static final Media MEDIA = new Media(10);
    private static final Logger LOGGER = new Logger(MEDIA);

    private static void addStations() {
        for (int i = 0; i < 15; ++i) {
            IPv4Address ip = IPv4Address.createRandom();
            Pos pos = Pos.createRandom(new Pos(0, 0), new Pos(10, 10));
            MEDIA.addStation("S%02d".formatted(i + 1), pos, 4, ip);
        }
    }

    private static void tick() {
        for (int i = 0; i < 1000000; ++i) {
            MEDIA.tick();
        }
    }

    private static void dump() {
        try {
            LOGGER.dumpEvents();
            LOGGER.dumpStations();
            LOGGER.dumpRouteTables();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void test() {
        NetworkLayer.RECORD = true;
        DataLayer.RECORD = true;
        PhysicalLayer.RECORD = false;

        addStations();
        tick();
        dump();
    }

    public static void main(String[] args) {
        test();
    }
}
