package cool.muyucloud.mnsimulator.models.layers;

import cool.muyucloud.mnsimulator.data.Data;

public interface Layer {
    /**Layer behaviour that tries to send a data pack
     * Should be called by Station::receiveTick()
     * */
    void sendTick();

    /**Layer behaviour that deals with received data pack
     * Should be called by Station::receiveTick()
     * */
    void receiveTick();

    /** Send a data pack and let layer deal with it
     * If a data pack was an instance that this layer can handle
     * the layer will handle it internally
     * If not, the layer will forward the pack to upper layer
     * @param data should not be corresponding data class dealt in lower layer
     */
    void receive(Data data);

    /** Offer a data pack to broadcast
     * layer will get the pack sealed and forward to lower layer
     * @param data should be corresponding data class dealt in upper layer
     * */
    void offer(Data data);
}
