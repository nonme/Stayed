package dev.hearthbound.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class HearthboundCommand extends AbstractCommandCollection {

    public HearthboundCommand() {
        super("hb", "Hearthbound mod commands");

        addSubCommand(new SpawnCommand());
        addSubCommand(new BlockCommand());
        addSubCommand(new BuildCommand());
        addSubCommand(new SaveCommand());
        addSubCommand(new LoadCommand());
        addSubCommand(new DialogCommand());
        addSubCommand(new HudCommand());
        addSubCommand(new ResetCommand());
        addSubCommand(new CosmeticsCommand());
        addSubCommand(new SkinCommand());
        addSubCommand(new DebugCommand());
        addSubCommand(new PrefabModeCommand());
        addSubCommand(new FastBuildCommand());
        addSubCommand(new InstaBuildCommand());
        addSubCommand(new SpawnVillagerCommand());
        addSubCommand(new TestVillagersCommand());
        addSubCommand(new TestSkinTonesCommand());
        addSubCommand(new QuestCommand());
        addSubCommand(new NpcCommand());
        addSubCommand(new TimeCommand());
        addSubCommand(new DoorsCommand());
        addSubCommand(new CraftabilityCommand());
        addSubCommand(new WarpCommand());
    }
}
