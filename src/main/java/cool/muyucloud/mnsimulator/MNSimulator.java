package cool.muyucloud.mnsimulator;

import cool.muyucloud.mnsimulator.models.Media;
import cool.muyucloud.mnsimulator.models.Station;
import cool.muyucloud.mnsimulator.models.layers.DataLayer;
import cool.muyucloud.mnsimulator.models.layers.NetworkLayer;
import cool.muyucloud.mnsimulator.models.protocols.LeachProtocol;
import cool.muyucloud.mnsimulator.models.protocols.NetworkLayerProtocol;
import cool.muyucloud.mnsimulator.util.IPv4Address;
import cool.muyucloud.mnsimulator.util.Logger;
import cool.muyucloud.mnsimulator.util.Pos;

import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.Objects;
import java.util.Random;

public class MNSimulator {
    public static final long START_TIME = LocalTime.now().getLong(ChronoField.MILLI_OF_SECOND);
    public static final Random RANDOM = new Random(0);
    public static final Media MEDIA = new Media(100);
    public static final Logger LOGGER = new Logger(MEDIA);

    private static void init() {
        NetworkLayer.RECORD = true;
        DataLayer.RECORD = false;

        Station.PROTOCOLS.add(NetworkLayerProtocol.class);
        Station.PROTOCOLS.add(LeachProtocol.class);

        Pos src = new Pos(0, 0), dst = new Pos(40, 40);
        for (int i = 0; i < 40; ++i) {
            Pos pos = Pos.createRandom(src, dst);
            IPv4Address ip = IPv4Address.createRandom();
            MEDIA.addStation("S%02d".formatted(i + 1), pos, 10, ip);
        }
    }

    private static void run() {
        for (int i = 0; i < 20000; ++i) {
            MEDIA.tick();
        }
    }

    private static void record() {
        try {
            LOGGER.dumpEvents();
            LOGGER.dumpStations();
            LOGGER.dumpRouteTables();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        init();
        run();
        record();
    }
}
