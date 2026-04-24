package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.npc.VillagerAppearance;
import dev.hearthbound.npc.appearance.BodyArchetype;
import dev.hearthbound.npc.appearance.StyleArchetype;
import it.unimi.dsi.fastutil.Pair;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns a row of throwaway Villager_Human NPCs for visual verification of
 * {@link VillagerAppearance}. Each NPC is given an index from 0..count-1 so the
 * archetype gate runs as it would for a real village — the first few are forced
 * Peasants, later ones may roll Citizen / Exotic.
 *
 * <p>No persistence: the NPCs are spawned raw, without {@code VillagerData}, and
 * are not counted toward {@code VillageData.villagerCount}. Each invocation starts
 * the index sequence from zero again.
 */
public class TestVillagersCommand extends AbstractPlayerCommand {

    private static final String VILLAGER_ROLE = "Villager_Human";
    private static final double SPACING_X = 2.0;

    private final RequiredArg<Integer> countArg;

    public TestVillagersCommand() {
        super("testvillagers", "Spawn a row of N test villagers (0..N-1) to preview archetypes");
        countArg = withRequiredArg("count", "Number of villagers to spawn (e.g. 20)", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                           PlayerRef player, World world) {
        int count = ctx.get(countArg);
        if (count < 1 || count > 100) {
            ctx.sendMessage(Message.raw("Count must be between 1 and 100."));
            return;
        }

        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            ctx.sendMessage(Message.raw("Could not get player position."));
            return;
        }
        Vector3d base = transform.getPosition();

        int peasants = 0, citizens = 0, exotics = 0, fails = 0;
        for (int index = 0; index < count; index++) {
            long seed = ThreadLocalRandom.current().nextLong();
            Vector3d pos = new Vector3d(base.getX() + 2.0 + index * SPACING_X, base.getY(), base.getZ() + 2.0);

            Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                    NpcManager.spawnNpc(store, pos, new Vector3f(0, 0, 0), VILLAGER_ROLE);
            if (result == null) {
                fails++;
                continue;
            }

            VillagerAppearance.apply(result.first(), store, seed, index);

            StyleArchetype style = VillagerAppearance.predictStyle(seed, index);
            BodyArchetype body = VillagerAppearance.predictBody(seed);
            switch (style) {
                case Peasant -> peasants++;
                case Citizen -> citizens++;
                case Exotic -> exotics++;
            }
            ctx.sendMessage(Message.raw(
                    "  idx=" + index + "  " + body + " " + style + "  seed=" + seed));
        }

        ctx.sendMessage(Message.raw(
                "Spawned " + (count - fails) + "/" + count + " test villagers — "
                        + peasants + " Peasant, " + citizens + " Citizen, " + exotics + " Exotic"
                        + (fails > 0 ? " (" + fails + " failed)" : "")));
    }
}
