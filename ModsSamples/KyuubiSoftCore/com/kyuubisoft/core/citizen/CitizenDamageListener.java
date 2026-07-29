/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ArchetypeChunk
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.SystemGroup
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.server.core.HytaleServer
 *  com.hypixel.hytale.server.core.command.system.CommandManager
 *  com.hypixel.hytale.server.core.command.system.CommandSender
 *  com.hypixel.hytale.server.core.console.ConsoleSender
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage
 *  com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem
 *  com.hypixel.hytale.server.core.modules.entity.damage.DamageModule
 *  com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
 *  com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue
 *  com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class CitizenDamageListener
extends DamageEventSystem {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Citizens");

    @Nonnull
    public Query<EntityStore> getQuery() {
        return Query.and((Query[])new Query[]{UUIDComponent.getComponentType()});
    }

    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage event) {
        try {
            if (event.isCancelled()) {
                return;
            }
            float damageAmount = event.getAmount();
            if (damageAmount <= 0.0f) {
                return;
            }
            Ref victimRef = chunk.getReferenceTo(index);
            if (victimRef == null || !victimRef.isValid()) {
                return;
            }
            UUIDComponent uuidComponent = (UUIDComponent)store.getComponent(victimRef, UUIDComponent.getComponentType());
            if (uuidComponent == null) {
                return;
            }
            UUID entityUUID = uuidComponent.getUuid();
            if (entityUUID == null) {
                return;
            }
            CitizenService service = CitizenService.getInstance();
            if (service == null) {
                return;
            }
            CitizenData citizen = service.getCitizenByUUID(entityUUID);
            if (citizen == null) {
                return;
            }
            if (!citizen.takesDamage) {
                event.setCancelled(true);
                event.setAmount(0.0f);
                try {
                    event.removeMetaObject(Damage.KNOCKBACK_COMPONENT);
                }
                catch (Exception exception) {
                    // empty catch block
                }
                return;
            }
            EntityStatMap statMap = (EntityStatMap)store.getComponent(victimRef, EntityStatMap.getComponentType());
            if (statMap == null) {
                event.setCancelled(true);
                return;
            }
            EntityStatValue healthStat = statMap.get(DefaultEntityStatTypes.getHealth());
            if (healthStat == null) {
                event.setCancelled(true);
                return;
            }
            float currentHealth = healthStat.get();
            if (currentHealth - damageAmount <= 0.0f) {
                event.setCancelled(true);
                if (CoreAPI.isDebug()) {
                    LOGGER.info("Citizen " + citizen.id + " received lethal damage (" + damageAmount + " dmg, " + currentHealth + " HP) \u2014 despawning");
                }
                this.handleCitizenDeath(citizen, service);
            }
        }
        catch (Exception e) {
            LOGGER.fine("CitizenDamageListener error: " + e.getMessage());
        }
    }

    private void handleCitizenDeath(CitizenData citizen, CitizenService service) {
        CommandManager cmdManager;
        String citizenId = citizen.id;
        String worldName = citizen.worldName;
        try {
            World world = this.getWorldByName(worldName);
            if (world != null) {
                service.despawnCitizen(citizen, world);
            } else {
                service.despawnCitizen(citizen);
            }
        }
        catch (Exception e) {
            LOGGER.fine("Citizen death despawn error: " + e.getMessage());
            service.despawnCitizen(citizen);
        }
        service.dispatchCitizenDeath(citizenId);
        if (citizen.deathCommands != null && !citizen.deathCommands.isEmpty() && (cmdManager = CommandManager.get()) != null) {
            for (CitizenData.CommandAction cmd : citizen.deathCommands) {
                if (cmd.command == null || cmd.command.isBlank()) continue;
                try {
                    String resolved = cmd.command.replace("{citizen}", citizenId).replace("{citizenname}", citizen.name != null ? citizen.name : citizenId);
                    cmdManager.handleCommand((CommandSender)ConsoleSender.INSTANCE, resolved);
                }
                catch (Exception e) {
                    LOGGER.warning("Death command failed for citizen " + citizenId + ": " + e.getMessage());
                }
            }
        }
        if (citizen.respawnOnDeath) {
            float delaySeconds = Math.max(1.0f, citizen.respawnDelay);
            long delayMs = (long)(delaySeconds * 1000.0f);
            LOGGER.fine("Scheduling respawn for citizen " + citizenId + " in " + delaySeconds + "s");
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                try {
                    CitizenData respawnCitizen = service.getCitizen(citizenId);
                    if (respawnCitizen == null) {
                        LOGGER.fine("Citizen " + citizenId + " no longer exists, skipping respawn");
                        return;
                    }
                    if (respawnCitizen.spawnedEntityUUID != null) {
                        LOGGER.fine("Citizen " + citizenId + " already spawned, skipping respawn");
                        return;
                    }
                    World respawnWorld = this.getWorldByName(worldName);
                    if (respawnWorld == null) {
                        LOGGER.warning("World " + worldName + " not found for citizen respawn: " + citizenId);
                        return;
                    }
                    respawnWorld.execute(() -> {
                        try {
                            service.spawnCitizen(respawnCitizen, respawnWorld);
                            LOGGER.fine("Citizen " + citizenId + " respawned successfully");
                        }
                        catch (Exception e) {
                            LOGGER.warning("Citizen respawn error for " + citizenId + ": " + e.getMessage());
                        }
                    });
                }
                catch (Exception e) {
                    LOGGER.warning("Citizen respawn scheduling error for " + citizenId + ": " + e.getMessage());
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    private World getWorldByName(String worldName) {
        if (worldName == null) {
            return null;
        }
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return null;
            }
            World defaultWorld = universe.getDefaultWorld();
            if (defaultWorld != null && CitizenService.matchesWorld(worldName, defaultWorld.getName())) {
                return defaultWorld;
            }
            return defaultWorld;
        }
        catch (Exception e) {
            LOGGER.fine("getWorldByName error: " + e.getMessage());
            return null;
        }
    }
}

