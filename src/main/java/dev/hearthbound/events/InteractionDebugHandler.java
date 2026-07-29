package dev.hearthbound.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionChainData;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
public final class InteractionDebugHandler {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("interact");
    private static PacketFilter inboundFilter;

    private InteractionDebugHandler() {}

    public static synchronized void registerPacketWatcher() {
        if (inboundFilter != null) return;
        inboundFilter = PacketAdapters.registerInbound(new DebugPacketWatcher());
        LOG.debug("inbound SyncInteractionChains watcher registered");
    }

    public static synchronized void unregisterPacketWatcher() {
        if (inboundFilter == null) return;
        PacketAdapters.deregisterInbound(inboundFilter);
        inboundFilter = null;
        LOG.debug("inbound SyncInteractionChains watcher unregistered");
    }

    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (event == null) return;
        LOG.with("action", event.getActionType())
           .with("cancelled", event.isCancelled())
           .with("player", playerId(event))
           .with("block", format(event.getTargetBlock()))
           .with("targetRef", format(event.getTargetRef()))
           .with("targetEntity", format(event.getTargetEntity()))
           .with("item", String.valueOf(event.getItemInHand()))
           .debug("interact event");
    }

    public static void onPlayerMouseButton(PlayerMouseButtonEvent event) {
        if (event == null) return;
        LOG.with("button", event.getMouseButton())
           .with("cancelled", event.isCancelled())
           .with("player", playerId(event))
           .with("block", format(event.getTargetBlock()))
           .with("targetEntity", format(event.getTargetEntity()))
           .with("item", String.valueOf(event.getItemInHand()))
           .debug("mouse event");
    }

    private static String playerId(PlayerInteractEvent event) {
        return event.getPlayer() != null ? String.valueOf(event.getPlayer().getUuid()) : "null";
    }

    private static String playerId(PlayerMouseButtonEvent event) {
        return event.getPlayer() != null ? String.valueOf(event.getPlayer().getUuid()) : "null";
    }

    private static String format(Vector3i block) {
        return block == null ? "null" : block.x + "," + block.y + "," + block.z;
    }

    private static String format(Ref<EntityStore> ref) {
        return ref == null ? "null" : "valid=" + ref.isValid() + " " + ref;
    }

    private static String format(Entity entity) {
        if (entity == null) return "null";
        return entity.getClass().getSimpleName() + ":" + entity.getUuid();
    }

    private static final class DebugPacketWatcher implements PacketWatcher {
        @Override
        public void accept(PacketHandler packetHandler, Packet packet) {
            if (!(packet instanceof SyncInteractionChains chains)) return;
            try {
                UUID playerUuid = packetHandler != null && packetHandler.getAuth() != null
                        ? packetHandler.getAuth().getUuid() : null;
                SyncInteractionChain[] updates = chains.updates;
                if (updates == null) {
                    LOG.with("player", playerUuid).debug("interact-packet: updates=null");
                    return;
                }
                PlayerRef playerRef = playerUuid != null ? Universe.get().getPlayer(playerUuid) : null;
                Store<EntityStore> store = playerRef != null && playerRef.isValid()
                        && playerRef.getReference() != null
                        ? playerRef.getReference().getStore() : null;
                for (int i = 0; i < updates.length; i++) {
                    logChain(playerUuid, store, i, updates[i]);
                }
            } catch (Exception e) {
                LOG.warn("failed to inspect SyncInteractionChains", e);
            }
        }

        private static void logChain(UUID playerUuid, Store<EntityStore> store,
                                     int index, SyncInteractionChain chain) {
            if (chain == null) {
                LOG.with("player", playerUuid)
                   .with("index", index)
                   .debug("interact-packet: chain=null");
                return;
            }
            InteractionChainData data = chain.data;
            Ref<EntityStore> targetRef = resolveTarget(store, data);
            LOG.with("player", playerUuid)
               .with("index", index)
               .with("type", chain.interactionType)
               .with("state", chain.state)
               .with("initial", chain.initial)
               .with("desync", chain.desync)
               .with("chainId", chain.chainId)
               .with("item", chain.itemInHandId)
               .with("dataEntityId", data != null ? data.entityId : null)
               .with("dataBlock", data != null ? String.valueOf(data.blockPosition) : null)
               .with("target", InteractionDebugHandler.format(targetRef))
               .with("targetUuid", uuidOf(store, targetRef))
               .with("sync", summarizeSyncData(chain.interactionData))
               .debug("interact-packet chain");
        }

        private static Ref<EntityStore> resolveTarget(Store<EntityStore> store,
                                                      InteractionChainData data) {
            if (store == null || store.getExternalData() == null || data == null) return null;
            if (data.entityId == 0) return null;
            return store.getExternalData().getRefFromNetworkId(data.entityId);
        }

        private static UUID uuidOf(Store<EntityStore> store, Ref<EntityStore> ref) {
            if (store == null || ref == null || !ref.isValid()) return null;
            UUIDComponent uuid = store.getComponent(ref, UUIDComponent.getComponentType());
            return uuid != null ? uuid.getUuid() : null;
        }

        private static String summarizeSyncData(InteractionSyncData[] syncData) {
            if (syncData == null) return "null";
            StringBuilder out = new StringBuilder("[");
            int limit = Math.min(syncData.length, 4);
            for (int i = 0; i < limit; i++) {
                if (i > 0) out.append(';');
                InteractionSyncData data = syncData[i];
                if (data == null) {
                    out.append("null");
                } else {
                    out.append("state=").append(data.state)
                            .append(",entity=").append(data.entityId)
                            .append(",block=").append(data.blockPosition)
                            .append(",root=").append(data.rootInteraction)
                            .append(",op=").append(data.operationCounter);
                }
            }
            if (syncData.length > limit) out.append(";...+").append(syncData.length - limit);
            return out.append(']').toString();
        }

    }
}
