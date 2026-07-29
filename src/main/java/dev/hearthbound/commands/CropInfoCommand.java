package dev.hearthbound.commands;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * PoC command: dumps everything we can read about the block the player is looking at,
 * or in a small area around them. Used to verify whether we can detect:
 *   - crop growth stage (Stage1..StageFinal) for natively-grown crops
 *   - tilled-but-empty soil
 *   - watered/fertilized soil state
 *   - blocks the player planted themselves (e.g. pumpkin)
 *
 * Usage:
 *   /hb cropinfo                   → scans 5x3x5 around the player's feet
 *   /hb cropinfo radius=N          → scans (2N+1) x 3 x (2N+1)
 *   /hb cropinfo here              → just the block under the player
 *
 * Prints to chat AND to server log.
 */
public class CropInfoCommand extends AbstractPlayerCommand {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("cmd.cropinfo");
    private final DefaultArg<String> modeArg;

    public CropInfoCommand() {
        super("cropinfo", "Dump block info around the player (PoC for farming detection)");
        modeArg = withDefaultArg("mode", "Mode: 'scan' (default radius=8), 'here', or 'radius=N'",
                ArgTypes.STRING, "scan", "scan");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                           PlayerRef player, World world) {
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            ctx.sendMessage(Message.raw("No player position."));
            return;
        }

        Vector3d pos = transform.getPosition();
        int px = (int) Math.floor(pos.getX());
        int py = (int) Math.floor(pos.getY());
        int pz = (int) Math.floor(pos.getZ());

        String mode = ctx.get(modeArg);
        int radius;
        if ("here".equals(mode)) {
            radius = 0;
        } else if (mode != null && mode.startsWith("radius=")) {
            try {
                radius = Math.max(0, Math.min(16, Integer.parseInt(mode.substring(7))));
            } catch (NumberFormatException e) {
                radius = 8;
            }
        } else {
            radius = 8;
        }

        List<String> reportLines = new ArrayList<>();
        // Scan a 5-block-tall slab so we catch tall crops (trees, corn, etc.):
        // floor (y-1) up through y+3.
        for (int dy = -1; dy <= 3; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int x = px + dx;
                    int y = py + dy;
                    int z = pz + dz;
                    String line = describeBlock(world, x, y, z);
                    if (line != null) reportLines.add(line);
                }
            }
        }

        // Group counts per block ID for the chat summary
        Map<String, Integer> summary = new LinkedHashMap<>();
        for (String line : reportLines) {
            int closeBracket = line.indexOf("] ");
            if (closeBracket < 0) continue;
            int afterId = line.indexOf(' ', closeBracket + 2);
            String blockId = afterId < 0 ? line.substring(closeBracket + 2)
                                         : line.substring(closeBracket + 2, afterId);
            summary.merge(blockId, 1, Integer::sum);
        }

        Path outPath = Path.of("cropinfo_dump.txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(outPath.toFile()))) {
            out.println("=== cropinfo at " + px + "," + py + "," + pz
                    + " radius=" + radius + " ===");
            out.println();
            out.println("Summary (" + reportLines.size() + " blocks):");
            for (Map.Entry<String, Integer> e : summary.entrySet()) {
                out.println("  " + e.getValue() + "x  " + e.getKey());
            }
            out.println();
            out.println("Full dump:");
            for (String line : reportLines) {
                out.println(line);
            }
        } catch (Exception e) {
            ctx.sendMessage(Message.raw("Failed to write dump: " + e.getMessage()));
            LOG.warn("CropInfo dump failed: " + e);
            return;
        }

        ctx.sendMessage(Message.raw("Cropinfo: " + reportLines.size()
                + " blocks scanned, " + summary.size() + " unique types"));
        ctx.sendMessage(Message.raw("Saved to: " + outPath.toAbsolutePath()));
        LOG.info("CropInfo dump saved to: " + outPath.toAbsolutePath());
    }

    /**
     * Returns a one-line description of the block, or null if it's air/empty/uninteresting.
     * Also dumps full reflective state to the log for the FIRST non-empty block per scan.
     */
    private String describeBlock(World world, int x, int y, int z) {
        try {
            BlockType bt = world.getBlockType(x, y, z);
            if (bt == null) return null;
            String id = bt.getId();
            if (id == null) return null;
            if (id.equals("Empty") || id.equals("Editor_Empty") || id.equals("Filter_Air_Block")) return null;

            // Filter to farming-relevant blocks. State-variant blocks come with a leading
            // "*" prefix (e.g. *Plant_Crop_Tomato_Block_State_Definitions_StageFinal) — strip
            // it before matching so we catch grown crops, not just Stage1 base blocks.
            String idForMatch = id.startsWith("*") ? id.substring(1) : id;
            boolean relevant = idForMatch.startsWith("Plant_")
                    || idForMatch.contains("Tilled")
                    || idForMatch.startsWith("Soil_")
                    || idForMatch.contains("_Sapling")
                    || idForMatch.contains("_Seedling");
            if (!relevant) return null;

            StringBuilder sb = new StringBuilder();
            sb.append("[").append(x).append(",").append(y).append(",").append(z).append("] ");
            sb.append(id);

            // Try to read state via BlockType reflective probe
            String stateInfo = probeBlockTypeState(bt);
            if (stateInfo != null && !stateInfo.isEmpty()) {
                sb.append(" | bt:").append(stateInfo);
            }

            // Try to read any block-entity ChunkStore components
            String compInfo = probeBlockComponents(world, x, y, z);
            if (compInfo != null && !compInfo.isEmpty()) {
                sb.append(" | cs:").append(compInfo);
            }

            return sb.toString();
        } catch (Exception e) {
            return "[" + x + "," + y + "," + z + "] ERROR: " + e.getClass().getSimpleName() + " " + e.getMessage();
        }
    }

    /**
     * Reflectively dumps state-related info from BlockType.
     * We don't know the exact state API surface yet — try common getter names.
     */
    private String probeBlockTypeState(BlockType bt) {
        StringBuilder out = new StringBuilder();
        // Known/likely state-related getters on BlockType — try and skip silently if absent
        String[] candidates = {
                "getStateId", "getState", "getStateName", "getStateDefinition",
                "getDefaultStateName", "getStates", "getStateDefinitions"
        };
        for (String name : candidates) {
            try {
                Method m = bt.getClass().getMethod(name);
                Object val = m.invoke(bt);
                if (val != null) {
                    String s = String.valueOf(val);
                    if (s.length() > 80) s = s.substring(0, 77) + "...";
                    if (out.length() > 0) out.append(",");
                    out.append(name).append("=").append(s);
                }
            } catch (NoSuchMethodException ignored) {
                // expected for most names
            } catch (Exception e) {
                if (out.length() > 0) out.append(",");
                out.append(name).append("=ERR");
            }
        }
        return out.toString();
    }

    /**
     * Lists ChunkStore components on the block-component entity at (x,y,z).
     * For TilledSoil blocks we expect to see at least one component with relevant fields.
     */
    private String probeBlockComponents(World world, int x, int y, int z) {
        try {
            WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
            if (chunk == null) return "chunk_not_loaded";

            // Component holder route (works for both ticking and sleeping block-entities)
            Holder<ChunkStore> holder = chunk.getBlockComponentHolder(x, y, z);
            if (holder == null) {
                Ref<ChunkStore> ref = chunk.getBlockComponentEntity(x, y, z);
                if (ref == null) return "no_component_entity";
                return describeComponents(ref);
            }
            return describeHolderComponents(holder);
        } catch (Exception e) {
            return "ERR_" + e.getClass().getSimpleName();
        }
    }

    /** Walk all reflective methods of the holder and report non-null components. */
    private String describeHolderComponents(Holder<ChunkStore> holder) {
        // We don't know the iteration API for components on a Holder — dump toString for now
        // and let the user/test see the actual class structure.
        try {
            String s = String.valueOf(holder);
            if (s.length() > 200) s = s.substring(0, 197) + "...";
            return "holder=" + s;
        } catch (Exception e) {
            return "holder_err";
        }
    }

    private String describeComponents(Ref<ChunkStore> ref) {
        try {
            String s = String.valueOf(ref);
            if (s.length() > 200) s = s.substring(0, 197) + "...";
            return "ref=" + s;
        } catch (Exception e) {
            return "ref_err";
        }
    }
}
