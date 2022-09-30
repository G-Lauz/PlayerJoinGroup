package fr.freebuild.playerjoingroup.bungee.listener;

import fr.freebuild.playerjoingroup.bungee.Config;
import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.bungee.Utils;
import fr.freebuild.playerjoingroup.bungee.query.QueryHasPlayedBefore;
import fr.freebuild.playerjoingroup.bungee.query.QuerySpigotServer;
import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.*;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.*;
import java.util.concurrent.*;

public class PluginMessageReceiver implements Listener {
//
//    private final PlayerJoinGroup plugin;
//
//    private HashMap<Integer, QuerySpigotServer<Boolean>> subscribers;
//
//    public PluginMessageReceiver(PlayerJoinGroup plugin) {
//        this.plugin = plugin;
//        this.subscribers = new HashMap<>();
//    }
//
//    /**
//     * Called when there is an incoming Plugin Message Channel on the channel parameter configure in the fr.freebuild.playerjoingroup.core.config.properties.
//     * @param event The plugin message event
//     */
//    @EventHandler
//    public void on(PluginMessageEvent event) throws
//            DeconstructPacketErrorException, UnknownSubchannelException, UnknownGroupException {
//
//        if (!event.getTag().equalsIgnoreCase(this.plugin.getConfig().getChannel()))
//            return;
//
//        Packet packet = Protocol.deconstructPacket(event.getData());
//
//        String subchannel = packet.getSubchannel();
//
//        // Handle the subchannel
//        switch (Subchannel.valueOf(subchannel)) {
//            case BROADCAST:
//                this.plugin.getMessager().broadcast(packet);
//                break;
//
//            case EVENT:
//                String eventType = packet.getParams().get(ParamsKey.EVENT.getValue());
//                switch (EventType.typeof(eventType)) {
//                    case FIRST_SPIGOT_CONNECTION -> onFirstConnection(packet.getData());
//                    default -> this.plugin.getLogger().warning("Unknown event: " + eventType);
//                }
//                break;
//
//            case QUERY:
//                String queryType = packet.getParams().get(ParamsKey.QUERY.getValue());
//                switch (QueryType.typeof(queryType)) {
//                    case HAS_PLAYED_BEFORE_RESPONSE -> {
//                        int hashCode = Integer.parseInt(packet.getParams().get(ParamsKey.HASH_CODE.getValue()));
//                        boolean data = Boolean.parseBoolean(packet.getData());
//                        this.notifySubscriber(hashCode, data);
//                        this.unsubscribe(hashCode);
//                    }
//                    default -> this.plugin.getLogger().warning("Unknown query: "+ queryType);
//                }
//                break;
//
//            default:
//                this.plugin.getLogger().warning("Received packet with unknown subchannel: " + subchannel);
//                throw new UnknownSubchannelException(subchannel);
//        }
//    }
//
//    private void onFirstConnection(String playerUUID) {
//        ProxiedPlayer player =  this.plugin.getProxy().getPlayer(UUID.fromString(playerUUID));
//        String serverName = player.getServer().getInfo().getName();
//        Config config = this.plugin.getConfig();
//
//        String group = Utils.getServerGroupName(serverName, config);
//
//        Collection<QuerySpigotServer<Boolean>> queries = new ArrayList<>();
//        ((ArrayList)config.getGroup().get(group)).forEach(server -> {
//            if (!server.equals(serverName)) {
//                ServerInfo serverInfo = this.plugin.getProxy().getServerInfo((String)server);
//
//                if (serverInfo == null) {
//                    plugin.getLogger().warning("Unable to find server \"" + serverName + "\", ignoring it.");
//                    return;
//                }
//                Collection<ProxiedPlayer> serverPlayers = serverInfo.getPlayers();
//                if (serverPlayers == null || serverPlayers.isEmpty())
//                    return;
//
//                QuerySpigotServer<Boolean> query = new QuerySpigotServer<>(serverInfo);
//                int queryHashCode = query.hashCode();
//
//                Packet packet = new Packet.Builder(Subchannel.QUERY)
//                        .setData(playerUUID)
//                        .setQuery(QueryType.HAS_PLAYED_BEFORE)
//                        .setServerGroup(group)
//                        .setHashCode(queryHashCode)
//                        .build();
//                query.setRequest(packet);
//
//                this.subscribe(queryHashCode, query);
//                queries.add(query);
//            }
//        });
//
//        ExecutorService service = Executors.newSingleThreadExecutor();
//        Future<Boolean> hasPlayedBefore = service.submit(new QueryHasPlayedBefore(queries));
//        try {
//            if (!hasPlayedBefore.get()) {
//                Packet greetingPacket = new Packet.Builder(Subchannel.EVENT)
//                        .setData(player.getName())
//                        .setEventType(EventType.FIRST_GROUP_CONNECTION)
//                        .setServerGroup(group)
//                        .build();
//                this.plugin.getMessager().broadcast(greetingPacket);
//            }
//        } catch (Exception e) {
//            this.plugin.getLogger().severe(Arrays.toString(e.getStackTrace()));
//            return;
//        }
//    }
//
//    private void subscribe(int hash, QuerySpigotServer<Boolean> subscriber) {
//        this.subscribers.put(hash, subscriber);
//    }
//
//    private void unsubscribe(int hash) {
//        this.subscribers.remove(hash);
//    }
//
//    private void notifySubscriber(int hash, boolean hasPlayedBefore) {
//        this.subscribers.get(hash).update(hasPlayedBefore);
//    }
}
