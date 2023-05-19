package cool.muyucloud.mnsimulator.data;

import cool.muyucloud.mnsimulator.util.Address;
import cool.muyucloud.mnsimulator.util.PlainAddress;
import org.jetbrains.annotations.NotNull;

public class PlainData implements Data {
    private final int size;

    public PlainData(int size) {
        this.size = size;
    }

    public PlainData() {
        this.size = 0;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public Data data() {
        return this;
    }

    @Override
    public @NotNull Address sender() {
        return new PlainAddress("");
    }

    @Override
    public @NotNull Address target() {
        return new PlainAddress("");
    }

    @Override
    public String desc() {
        return "NULL";
    }

    @Override
    public String dataType() {
        return "Data";
    }
}
