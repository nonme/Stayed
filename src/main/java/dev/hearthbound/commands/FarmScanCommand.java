package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.building.FarmBounds;
import dev.hearthbound.building.FarmScanner;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.logging.Logger;

/**
 * Reports each farm's bbox and scan result. Used to verify FarmBounds rotation handling
 * and FarmScanner classification without involving any NPC behavior.
 */
public class FarmScanCommand extends AbstractPlayerCommand {

    private static final Logger LOGGER = Logger.getLogger(FarmScanCommand.class.getName());

    public FarmScanCommand() {
        super("farmscan", "Print bbox and scan results for every farm in the village");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                           PlayerRef player, World world) {
        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null || !village.isFounded()) {
            ctx.sendMessage(Message.raw("No village."));
            return;
        }

        int farmCount = 0;
        for (BuildingRecord b : village.getBuildings()) {
            if (!BuildingType.FARM.equals(b.getType())) continue;
            farmCount++;
            reportFarm(ctx, world, b);
        }
        if (farmCount == 0) {
            ctx.sendMessage(Message.raw("No farms found."));
        }
    }

    private void reportFarm(CommandContext ctx, World world, BuildingRecord farm) {
        String header = "Farm @ " + farm.getPosX() + "," + farm.getPosY() + "," + farm.getPosZ()
                + " rot=" + farm.getRotation()
                + (farm.isCompleted() ? " [completed]" : " [under construction]");
        ctx.sendMessage(Message.raw(header));
        LOGGER.info(header);

        FarmBounds.Bounds b = FarmBounds.compute(farm);
        if (b == null) {
            ctx.sendMessage(Message.raw("  (no bounds)"));
            return;
        }
        String bboxLine = "  bbox x[" + b.minX() + ".." + b.maxX() + "]"
                + " y[" + b.minY() + ".." + b.maxY() + "]"
                + " z[" + b.minZ() + ".." + b.maxZ() + "]"
                + " size=" + b.width() + "x" + b.height() + "x" + b.depth();
        ctx.sendMessage(Message.raw(bboxLine));
        LOGGER.info(bboxLine);

        FarmScanner.Scan scan = FarmScanner.scan(world, farm);
        String summary = "  scan: harvest=" + scan.harvest().size()
                + " weed=" + scan.weed().size()
                + " till=" + scan.till().size()
                + " replant=" + scan.replant().size()
                + " water=" + scan.water().size();
        ctx.sendMessage(Message.raw(summary));
        LOGGER.info(summary);

        // Print first few of each category — full detail goes to log.
        printSample(ctx, "harvest", scan.harvest());
        printSample(ctx, "weed",    scan.weed());
        printSample(ctx, "till",    scan.till());
        printSample(ctx, "replant", scan.replant());
        printSample(ctx, "water",   scan.water());
    }

    private void printSample(CommandContext ctx, String label, java.util.List<FarmScanner.Target> list) {
        int max = 3;
        for (int i = 0; i < Math.min(max, list.size()); i++) {
            FarmScanner.Target t = list.get(i);
            String line = "    " + label + ": " + t.x() + "," + t.y() + "," + t.z()
                    + (t.cropType() != null ? " (" + t.cropType() + ")" : "");
            ctx.sendMessage(Message.raw(line));
            LOGGER.info(line);
        }
        if (list.size() > max) {
            ctx.sendMessage(Message.raw("    " + label + ": ... (+" + (list.size() - max) + " more)"));
        }
    }
}
