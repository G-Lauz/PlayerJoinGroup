package fr.freebuild.playerjoingroup.bungee.listener;

import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.core.protocol.*;

import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PluginMessageReceiver implements Listener {

    private final PlayerJoinGroup plugin;

    public PluginMessageReceiver(PlayerJoinGroup plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when their is an incoming Plugin Message Channel on the channel parameter configure in the fr.freebuild.playerjoingroup.core.config.properties.
     * @param event The plugin message event
     */
    @EventHandler
    public void on(PluginMessageEvent event) throws
            DeconstructPacketErrorException, UnknownSubchannelException, UnknownGroupException {

        if (!event.getTag().equalsIgnoreCase(this.plugin.getConfig().getChannel()))
            return;

        Packet packet = Protocol.deconstructPacket(event.getData());
        String targetGroup = packet.getParams().get(ParamsKey.SERVER_GROUP.getValue());

        boolean notKnownGroup = !this.plugin.getConfig().getGroup().containsKey(targetGroup);
        if (notKnownGroup) {
            this.plugin.getLogger().warning("Received packet with unknown target group: " + targetGroup);
            throw new UnknownGroupException(targetGroup);
        }

        String subchannel = packet.getSubchannel();

        // Handle the subchannel
        switch (Subchannel.valueOf(subchannel)) {
            case BROADCAST:
                this.plugin.getMessager().broadcast(packet);
                break;

            case EVENT:
                // TODO
                break;

            default:
                this.plugin.getLogger().warning("Received packet with unknown subchannel: " + subchannel);
                throw new UnknownSubchannelException(subchannel);
        }
    }
}
