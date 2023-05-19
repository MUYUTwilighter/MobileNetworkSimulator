package cool.muyucloud.mnsimulator.data;

import cool.muyucloud.mnsimulator.util.Address;
import org.jetbrains.annotations.NotNull;

public interface Data {
    int size();

    Data data();

    @NotNull
    Address sender();

    @NotNull
    Address target();

    String desc();

    String dataType();
}
