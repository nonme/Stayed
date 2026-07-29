package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.building.BuildingLayout;
import dev.hearthbound.building.DoorPrefabResolver;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.List;

public class DoorsCommand extends AbstractCommandCollection {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("cmd.doors");

    public DoorsCommand() {
        super("doors", "Test door open/close for all village buildings");
        addSubCommand(new OpenDoorsCommand());
        addSubCommand(new CloseDoorsCommand());
    }

    private static void applyDoors(boolean open, CommandContext ctx,
                                   Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null || !village.isFounded()) {
            ctx.sendMessage(Message.raw("No village found."));
            return;
        }

        List<BuildingRecord> buildings = village.getBuildings();
        int count = 0;

        for (BuildingRecord b : buildings) {
            if (!b.isCompleted()) continue;
            BuildingLayout.Layout layout = BuildingLayout.get(b.getType(), b.getVariant());
            if (layout == null || layout.doors().isEmpty()) continue;

            int steps = layout.rotationSteps(b.getRotation());
            for (BuildingLayout.DoorPlacement door : layout.doors()) {
                int[] off = rotateLocalOffset(door.lx(), door.ly(), door.lz(), steps);
                int doorRot = door.worldRotation(b.getRotation(), layout.anchorPrefabRotation());
                int gx = b.getPosX() + off[0];
                int gy = b.getPosY() + off[1];
                int gz = b.getPosZ() + off[2];
                String stateBlock = open ? door.openBlock() : door.closeBlock();
                String prefabName = DoorPrefabResolver.resolve(stateBlock);
                if (prefabName == null) {
                    LOG.with("stateBlock", stateBlock)
                       .with("x", gx).with("y", gy).with("z", gz)
                       .warn("doors: no prefab for stateBlock — gate kind not supported");
                    continue;
                }

                final int fgx = gx, fgy = gy, fgz = gz, fRot = doorRot;
                final String fPrefab = prefabName;
                world.execute(() -> {
                    try {
                        BlockSelection selection = PrefabStore.get()
                                .getAssetPrefabFromAnyPack(fPrefab + ".prefab.json");
                        int angleDeg = (fRot * 90) % 360;
                        BlockSelection rotated = angleDeg == 0 ? selection
                                : selection.rotate(Axis.Y, angleDeg);
                        rotated.setAnchorAtWorldPos(0, 0, 0);
                        Store<EntityStore> liveStore = world.getEntityStore().getStore();
                        rotated.placeNoReturn(world, new Vector3i(fgx, fgy, fgz), liveStore);
                    } catch (Exception e) {
                        LOG.with("prefab", fPrefab)
                           .with("x", fgx).with("y", fgy).with("z", fgz)
                           .warn("doors: failed: " + e.getMessage(), e);
                    }
                });
            }
            count++;
        }

        ctx.sendMessage(Message.raw((open ? "Opened" : "Closed") + " doors for " + count + " buildings."));
    }

    private static int[] rotateLocalOffset(int lx, int ly, int lz, int steps) {
        for (int i = 0; i < steps; i++) {
            int tmp = lx;
            lx = lz;
            lz = -tmp;
        }
        return new int[]{lx, ly, lz};
    }

    static class OpenDoorsCommand extends AbstractPlayerCommand {
        OpenDoorsCommand() { super("open", "Open all village building doors"); }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store,
                               Ref<EntityStore> playerRef, PlayerRef player, World world) {
            applyDoors(true, ctx, store, playerRef, world);
        }
    }

    static class CloseDoorsCommand extends AbstractPlayerCommand {
        CloseDoorsCommand() { super("close", "Close all village building doors"); }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store,
                               Ref<EntityStore> playerRef, PlayerRef player, World world) {
            applyDoors(false, ctx, store, playerRef, world);
        }
    }
}
