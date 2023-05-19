package cool.muyucloud.mnsimulator.util;

import cool.muyucloud.mnsimulator.data.Data;
import cool.muyucloud.mnsimulator.models.Station;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;

public record Event(int tick, Station station, String state, String sender, String target, String dataType, int size, String desc) {
    public static Event log(int tick, Station station, String state, Data data) {
        String sender = data.sender().toString();
        String target = data.target().toString();
        return new Event(tick, station, state, sender, target, data.dataType(), data.size(), data.desc());
    }

    @Override
    public String toString() {
        return "%d, %s, %s, %s, %s, %s, %d, %s".formatted(tick, station.getName(), state, sender, target, dataType, size, desc);
    }

    public static String columns() {
        StringBuilder builder = new StringBuilder();
        Iterator<Field> fields = Arrays.stream(Event.class.getDeclaredFields()).iterator();
        while (fields.hasNext()) {
            builder.append(fields.next().getName());
            if (fields.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }
}
