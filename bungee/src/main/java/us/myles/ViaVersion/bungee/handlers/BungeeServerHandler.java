package us.myles.ViaVersion.bungee.handlers;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import us.myles.ViaVersion.api.Pair;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.boss.BossBar;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.protocol.ProtocolPipeline;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.bungee.service.ProtocolDetectorService;
import us.myles.ViaVersion.bungee.storage.BungeeStorage;
import us.myles.ViaVersion.protocols.base.ProtocolInfo;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.storage.EntityTracker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class BungeeServerHandler implements Listener {
    private static Method getPendingConnection;
    private static Method getHandshake;
    private static Method setProtocol;
    private static Method getEntityMap = null;
    private static Method setVersion = null;
    private static Field entityRewrite = null;
    private static Field channelWrapper = null;

    static {
        try {
            getPendingConnection = Class.forName("net.md_5.bungee.UserConnection").getDeclaredMethod("getPendingConnection");
            getHandshake = Class.forName("net.md_5.bungee.connection.InitialHandler").getDeclaredMethod("getHandshake");
            setProtocol = Class.forName("net.md_5.bungee.protocol.packet.Handshake").getDeclaredMethod("setProtocolVersion", int.class);
            getEntityMap = Class.forName("net.md_5.bungee.entitymap.EntityMap").getDeclaredMethod("getEntityMap", int.class);
            setVersion = Class.forName("net.md_5.bungee.netty.ChannelWrapper").getDeclaredMethod("setVersion", int.class);
            channelWrapper = Class.forName("net.md_5.bungee.UserConnection").getDeclaredField("ch");
            channelWrapper.setAccessible(true);
            entityRewrite = Class.forName("net.md_5.bungee.UserConnection").getDeclaredField("entityRewrite");
            entityRewrite.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Set the handshake version every time someone connects to any server
    @EventHandler
    public void onServerConnect(ServerConnectEvent e) {
        UserConnection user = Via.getManager().getConnection(e.getPlayer().getUniqueId());
        if (user == null) return;
        if (!user.has(BungeeStorage.class)) {
            user.put(new BungeeStorage(user, e.getPlayer()));
        }

        int protocolId = ProtocolDetectorService.getProtocolId(e.getTarget().getName());
        List<Pair<Integer, Protocol>> protocols = ProtocolRegistry.getProtocolPath(user.get(ProtocolInfo.class).getProtocolVersion(), protocolId);

        // Check if ViaVersion can support that version
        try {
            Object pendingConnection = getPendingConnection.invoke(e.getPlayer());
            Object handshake = getHandshake.invoke(pendingConnection);
            setProtocol.invoke(handshake, protocols == null ? user.get(ProtocolInfo.class).getProtocolVersion() : protocolId);
        } catch (InvocationTargetException | IllegalAccessException e1) {
            e1.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerConnected(ServerConnectedEvent e) {
        try {
            checkServerChange(e, Via.getManager().getConnection(e.getPlayer().getUniqueId()));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void checkServerChange(ServerConnectedEvent e, UserConnection user) throws Exception {
        if (user == null) return;
        // Manually hide ViaVersion-created BossBars if the childserver was version 1.8.x (#666)
        if (user.has(EntityTracker.class)) {
            EntityTracker tracker = user.get(EntityTracker.class);

            if (tracker.getBossBarMap() != null)
                for (BossBar bar : tracker.getBossBarMap().values())
                    bar.hide();
        }
        // Handle server/version change
        if (user.has(BungeeStorage.class)) {
            BungeeStorage storage = user.get(BungeeStorage.class);
            ProxiedPlayer player = storage.getPlayer();

            if (e.getServer() != null) {
                if (!e.getServer().getInfo().getName().equals(storage.getCurrentServer())) {
                    String serverName = e.getServer().getInfo().getName();

                    storage.setCurrentServer(serverName);

                    int protocolId = ProtocolDetectorService.getProtocolId(serverName);

                    ProtocolInfo info = user.get(ProtocolInfo.class);
                    // Refresh the pipes
                    List<Pair<Integer, Protocol>> protocols = ProtocolRegistry.getProtocolPath(info.getProtocolVersion(), protocolId);
                    ProtocolPipeline pipeline = user.get(ProtocolInfo.class).getPipeline();
                    user.clearStoredObjects();
                    pipeline.cleanPipes();
                    if (protocols == null) {
                        // TODO Check Bungee Supported Protocols? *shrugs*
                        protocolId = info.getProtocolVersion();
                    } else {
                        for (Pair<Integer, Protocol> prot : protocols) {
                            pipeline.add(prot.getValue());
                        }
                    }

                    user.put(info);
                    user.put(storage);

                    user.setActive(protocols != null);

                    // Init all protocols TODO check if this can get moved up to the previous for loop, and doesn't require the pipeline to already exist.
                    for (Protocol protocol : pipeline.pipes()) {
                        protocol.init(user);
                    }

                    Object wrapper = channelWrapper.get(player);
                    setVersion.invoke(wrapper, protocolId);

                    Object entityMap = getEntityMap.invoke(null, protocolId);
                    entityRewrite.set(player, entityMap);
                }
            }
        }
    }
}
