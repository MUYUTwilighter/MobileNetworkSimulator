package cool.muyucloud.mnsimulator.models.protocols;

import cool.muyucloud.mnsimulator.data.Data;
import cool.muyucloud.mnsimulator.models.layers.Layer;

public interface Protocol {
    void process(Data data);

    static Protocol create(Layer layer) {
        return null;
    }

    /**Show whether the protocol is available to be dispatched to
     * */
    boolean isBusy();

    Class<? extends Data> processable();
}
