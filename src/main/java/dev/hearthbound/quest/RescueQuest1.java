package dev.hearthbound.quest;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.npc.VillagerAppearance;
import dev.hearthbound.npc.VillagerNames;
import dev.hearthbound.village.VillagerData;
import it.unimi.dsi.fastutil.Pair;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/** Spawns the "trap with cheese" rescue encounter: prefab pit + trapped victim + a goblin guard.
 *
 *  <p>The prefab's anchor sits at (0, 0, 0) inside the file. Grass/surface is at prefab Y=8,
 *  spikes at Y=1-2, cheese lure at Y=11. We place the prefab so its surface row lines up
 *  with the world's ground level at the target block column. */
public final class RescueQuest1 {

    private static final Logger LOGGER = Logger.getLogger(RescueQuest1.class.getName());

    public static final String PREFAB_NAME = "Villager_ResqueQuest_trap";
    public static final String VICTIM_ROLE = "Villager_Rescue_Trapped";
    public static final String GUARD_ROLE = "Goblin_Scavenger";

    /** Prefab Y coordinate of the grass/surface row. */
    private static final int PREFAB_SURFACE_Y = 8;
    /** Victim spawn cell in prefab-local coords. (1, 2, 0) stands on the solid Rock_Stone
     *  floor at (1, 1, 0) with Empty above and no spike in that column — hand-picked from
     *  the prefab's block map. Victim role is also marked Invulnerable as a safety net. */
    private static final int VICTIM_DX = 1;
    private static final int VICTIM_PREFAB_Y = 2;
    private static final int VICTIM_DZ = 0;
    /** Guard offset from the pit center on the surface. Kept close enough that the player
     *  encounters the goblin before reaching the pit, but far enough from the 7x7 camouflage
     *  footprint (x,z in -3..3) that the goblin's wander is unlikely to drop it in. */
    private static final int GUARD_DX = 8;
    private static final int GUARD_DZ = 0;

    private RescueQuest1() {}

    /** Places the trap at {@code (centerX, groundY, centerZ)} and spawns NPCs.
     *  Must be called on the world thread with the target chunk already loaded. */
    public static Spawned spawn(World world, Store<EntityStore> store,
                                int centerX, int groundY, int centerZ) {
        // Place the prefab so its surface row (prefab Y=8) lands on the world's ground row.
        int prefabOriginWorldY = groundY - PREFAB_SURFACE_Y;

        try {
            BlockSelection selection = PrefabStore.get()
                    .getAssetPrefabFromAnyPack(PREFAB_NAME + ".prefab.json");
            selection.setAnchor(0, 0, 0);
            selection.placeNoReturn(world,
                    new Vector3i(centerX, prefabOriginWorldY, centerZ), store);
            LOGGER.info("Rescue trap prefab placed at center=(" + centerX + "," + groundY + "," + centerZ + ")");
        } catch (Exception e) {
            LOGGER.warning("Failed to place rescue trap prefab: " + e.getMessage());
            return null;
        }

        // Victim at the hand-picked safe cell in the pit.
        Vector3d victimPos = new Vector3d(
                centerX + VICTIM_DX + 0.5,
                prefabOriginWorldY + VICTIM_PREFAB_Y,
                centerZ + VICTIM_DZ + 0.5);
        Pair<Ref<EntityStore>, INonPlayerCharacter> victim =
                NpcManager.spawnNpcNoInteraction(store, victimPos, new Vector3f(0, 0, 0), VICTIM_ROLE);
        if (victim == null) {
            LOGGER.warning("Failed to spawn rescue victim");
        } else {
            applyVictimAppearance(victim.first(), store);
            NpcManager.assignRescueInteraction(store, victim.first());
        }

        // Guard on the surface near the pit.
        Vector3d guardPos = new Vector3d(
                centerX + GUARD_DX + 0.5,
                groundY + 1,
                centerZ + GUARD_DZ + 0.5);
        var guard = NpcManager.spawnNpcNoInteraction(store, guardPos, new Vector3f(0, 0, 0), GUARD_ROLE);
        if (guard == null) {
            LOGGER.warning("Failed to spawn rescue guard");
        }

        return new Spawned(victimPos, guardPos);
    }

    /** Attaches VillagerData + a seeded PlayerSkin so the rescue victim looks like any
     *  other human villager (not the default Outlander) and survives server restarts —
     *  {@link dev.hearthbound.events.VillageTickHandler#restoreVillagerSkins} rebuilds the
     *  skin from VillagerData.skinSeed on chunk reload. Index 0 forces the Peasant archetype
     *  (see SKINS.md), which matches the victim's "refugee / outcast" backstory. */
    private static void applyVictimAppearance(Ref<EntityStore> victimRef, Store<EntityStore> store) {
        long skinSeed = ThreadLocalRandom.current().nextLong();
        String[] name = VillagerNames.rollHumanName(skinSeed);
        VillagerData data = new VillagerData(VillagerData.RACE_HUMAN, name[0], name[1], skinSeed);
        store.putComponent(victimRef, VillagerData.getComponentType(), data);
        VillagerAppearance.apply(victimRef, store, skinSeed, 0);
    }

    /** Coordinates of the spawned victim and guard — handy if callers want to read them back
     *  (e.g. for placing the ReachLocationMarker exactly on the victim). */
    public record Spawned(Vector3d victimPos, Vector3d guardPos) {}
}
