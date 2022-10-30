package fr.freebuild.playerjoingroup.core.protocol;

/**
 * Base class of the chain of responsibility design pattern use to implement an OSI model like pattern for the
 * communication between BungeeCord and Spigot's servers.
 */
public abstract class Layer {

    protected Layer incomingSuccessorLayer;
    protected Layer outgoingSuccessorLayer;

    public abstract void handleIncomingRequest(int data);
    public abstract void handleOutgoingRequest(int data);

    /**
     *
     * @param incomingSuccessorLayer
     */
    public void setIncomingSuccessorLayer(Layer incomingSuccessorLayer) {
        this.incomingSuccessorLayer = incomingSuccessorLayer;
    }

    /**
     *
     * @param outgoingSuccessorLayer
     */
    public void setOutgoingSuccessorLayer(Layer outgoingSuccessorLayer) {
        this.outgoingSuccessorLayer = outgoingSuccessorLayer;
    }
}
