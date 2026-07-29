/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.codec.Codec
 *  com.hypixel.hytale.codec.KeyedCodec
 *  com.hypixel.hytale.codec.builder.BuilderCodec
 *  com.hypixel.hytale.codec.builder.BuilderCodec$Builder
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.RemoveReason
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
 *  com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.ui.DropdownEntryInfo
 *  com.hypixel.hytale.server.core.ui.LocalizableString
 *  com.hypixel.hytale.server.core.ui.builder.EventData
 *  com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
 *  com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.CorePlugin;
import com.kyuubisoft.core.citizen.CitizenAdminPage;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import com.kyuubisoft.core.citizen.NpcViewerEntry;
import com.kyuubisoft.core.citizen.NpcViewerScanner;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.i18n.I18nContext;
import com.kyuubisoft.core.util.CommandUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class NpcViewerPage
extends InteractiveCustomUIPage<PageEventData> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft NPC Viewer");
    private static final int ENTRIES_PER_PAGE = 16;
    private final CorePlugin plugin;
    private final Player player;
    private final PlayerRef playerRef;
    private final CitizenService citizenService;
    private Ref<EntityStore> storedRef;
    private Store<EntityStore> storedStore;
    private List<NpcViewerEntry> allEntries = new ArrayList<NpcViewerEntry>();
    private List<NpcViewerEntry> filteredEntries = new ArrayList<NpcViewerEntry>();
    private int listPage = 0;
    private String filterMode = "all";
    private String searchQuery = "";
    private int selectedIndex = -1;
    private boolean showingDespawnConfirm = false;
    private boolean dropdownInitialized = false;

    public NpcViewerPage(CorePlugin plugin, Player player, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageEventData.CODEC);
        this.plugin = plugin;
        this.player = player;
        this.playerRef = playerRef;
        this.citizenService = plugin.getCitizenService();
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder ui, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        I18nContext.run(this.playerRef, () -> {
            this.storedRef = ref;
            this.storedStore = store;
            ui.append("Pages/NpcViewer/NpcViewer.ui");
            this.setI18nLabels(ui);
            this.bindEvents(events);
            this.buildListPanel(ui);
            this.buildDetailPanel(ui);
            this.buildDespawnConfirm(ui);
            this.performScanAsync();
        });
    }

    private void setI18nLabels(UICommandBuilder ui) {
        CoreI18n i18n = CoreI18n.getInstance();
        ui.set("#NvTitleLabel.Text", i18n.get("npcviewer.title"));
        ui.set("#NvEmptyLabel.Text", i18n.get("npcviewer.no_selection"));
        ui.set("#NvRefreshLabel.Text", i18n.get("npcviewer.btn.refresh"));
        ui.set("#NvTeleportLabel.Text", i18n.get("npcviewer.btn.teleport"));
        ui.set("#NvSummonLabel.Text", i18n.get("npcviewer.btn.summon"));
        ui.set("#NvDespawnLabel.Text", i18n.get("npcviewer.btn.despawn"));
        ui.set("#NvOpenCitLabel.Text", i18n.get("npcviewer.btn.open_citizen"));
        ui.set("#NvDespawnConfirmTitle.Text", i18n.get("npcviewer.despawn_confirm_title"));
    }

    private void bindEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NvBackButton", EventData.of((String)"Button", (String)"back"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NvCloseButton", EventData.of((String)"Button", (String)"close"), false);
        for (int i = 0; i < 16; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#NvEntry" + i, EventData.of((String)"Button", (String)("select_" + i)), false);
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NvPrevBtn", EventData.of((String)"Button", (String)"nvPrev"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NvNextBtn", EventData.of((String)"Button", (String)"nvNext"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NvRefreshBtn", EventData.of((String)"Button", (String)"refresh"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NvTeleportBtn", EventData.of((String)"Button", (String)"teleport"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NvSummonBtn", EventData.of((String)"Button", (String)"summon"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NvDespawnBtn", EventData.of((String)"Button", (String)"despawn"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NvOpenCitBtn", EventData.of((String)"Button", (String)"openCitizen"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NvDespawnYesBtn", EventData.of((String)"Button", (String)"despawnYes"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NvDespawnNoBtn", EventData.of((String)"Button", (String)"despawnNo"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NvFilterDropdown", EventData.of((String)"@Filter", (String)"#NvFilterDropdown.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NvSearchField", EventData.of((String)"@Search", (String)"#NvSearchField.Value"), false);
    }

    private void refreshUI() {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        this.setI18nLabels(ui);
        this.bindEvents(events);
        this.buildListPanel(ui);
        this.buildDetailPanel(ui);
        this.buildDespawnConfirm(ui);
        this.sendUpdate(ui, events, false);
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData data) {
        super.handleDataEvent(ref, store, (Object)data);
        I18nContext.run(this.playerRef, () -> {
            this.storedRef = ref;
            this.storedStore = store;
            if (data.button != null) {
                this.handleButton(data.button);
                return;
            }
            if (data.filterValue != null) {
                if (!data.filterValue.equals(this.filterMode)) {
                    this.filterMode = data.filterValue;
                    this.listPage = 0;
                    this.selectedIndex = -1;
                    this.filterEntries();
                }
                this.refreshUI();
                return;
            }
            if (data.search != null) {
                this.searchQuery = data.search.toLowerCase().trim();
                this.listPage = 0;
                this.selectedIndex = -1;
                this.filterEntries();
                this.refreshUI();
                return;
            }
            this.refreshUI();
        });
    }

    private void handleButton(String button) {
        if (button.startsWith("select_")) {
            int slotIndex = Integer.parseInt(button.substring(7));
            int entryIndex = this.listPage * 16 + slotIndex;
            if (entryIndex >= 0 && entryIndex < this.filteredEntries.size()) {
                this.selectedIndex = entryIndex;
            }
            this.refreshUI();
            return;
        }
        switch (button) {
            case "back": 
            case "close": {
                this.close();
                return;
            }
            case "nvPrev": {
                if (this.listPage > 0) {
                    --this.listPage;
                }
                this.selectedIndex = -1;
                break;
            }
            case "nvNext": {
                int maxPage = Math.max(0, (this.filteredEntries.size() - 1) / 16);
                if (this.listPage < maxPage) {
                    ++this.listPage;
                }
                this.selectedIndex = -1;
                break;
            }
            case "refresh": {
                this.performScanAsync();
                return;
            }
            case "teleport": {
                this.handleTeleport();
                break;
            }
            case "summon": {
                this.handleSummon();
                return;
            }
            case "despawn": {
                if (this.selectedIndex < 0 || this.selectedIndex >= this.filteredEntries.size()) break;
                this.showingDespawnConfirm = true;
                break;
            }
            case "despawnYes": {
                this.handleDespawnConfirm();
                return;
            }
            case "despawnNo": {
                this.showingDespawnConfirm = false;
                break;
            }
            case "openCitizen": {
                this.handleOpenCitizenAdmin();
                return;
            }
        }
        this.refreshUI();
    }

    private void performScanAsync() {
        World world = this.player.getWorld();
        if (world == null) {
            return;
        }
        world.execute(() -> {
            try {
                this.allEntries = NpcViewerScanner.scan(world, this.citizenService);
                this.selectedIndex = -1;
                this.showingDespawnConfirm = false;
                this.filterEntries();
                this.refreshUI();
            }
            catch (Exception e) {
                LOGGER.warning("NPC scan failed: " + e.getMessage());
            }
        });
    }

    private void filterEntries() {
        this.filteredEntries = this.allEntries.stream().filter(e -> {
            if (!"all".equals(this.filterMode)) {
                switch (this.filterMode) {
                    case "registered": {
                        if (e.category() == NpcViewerEntry.NpcCategory.REGISTERED) break;
                        return false;
                    }
                    case "orphaned": {
                        if (e.category() == NpcViewerEntry.NpcCategory.ORPHANED) break;
                        return false;
                    }
                    case "world": {
                        if (e.category() == NpcViewerEntry.NpcCategory.WORLD) break;
                        return false;
                    }
                }
            }
            if (!this.searchQuery.isEmpty()) {
                String name = e.getDisplayName().toLowerCase();
                String type = e.npcTypeId() != null ? e.npcTypeId().toLowerCase() : "";
                String role = e.roleName() != null ? e.roleName().toLowerCase() : "";
                String citId = e.citizenId() != null ? e.citizenId().toLowerCase() : "";
                return name.contains(this.searchQuery) || type.contains(this.searchQuery) || role.contains(this.searchQuery) || citId.contains(this.searchQuery);
            }
            return true;
        }).collect(Collectors.toList());
        int maxPage = Math.max(0, (this.filteredEntries.size() - 1) / 16);
        if (this.listPage > maxPage) {
            this.listPage = maxPage;
        }
    }

    private void buildListPanel(UICommandBuilder ui) {
        CoreI18n i18n = CoreI18n.getInstance();
        ui.set("#NvScanInfo.Text", i18n.get("npcviewer.scan_info", String.valueOf(this.allEntries.size())));
        ui.set("#NvCountLabel.Text", this.filteredEntries.size() + " / " + this.allEntries.size());
        if (!this.dropdownInitialized) {
            this.dropdownInitialized = true;
            ui.set("#NvFilterDropdown.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)i18n.get("npcviewer.filter.all")), "all"), new DropdownEntryInfo(LocalizableString.fromString((String)i18n.get("npcviewer.filter.registered")), "registered"), new DropdownEntryInfo(LocalizableString.fromString((String)i18n.get("npcviewer.filter.orphaned")), "orphaned"), new DropdownEntryInfo(LocalizableString.fromString((String)i18n.get("npcviewer.filter.world")), "world")));
        }
        ui.set("#NvFilterDropdown.Value", this.filterMode);
        int startIdx = this.listPage * 16;
        for (int i = 0; i < 16; ++i) {
            int entryIdx = startIdx + i;
            boolean visible = entryIdx < this.filteredEntries.size();
            ui.set("#NvEntry" + i + ".Visible", visible);
            if (!visible) continue;
            NpcViewerEntry entry = this.filteredEntries.get(entryIdx);
            boolean isSelected = entryIdx == this.selectedIndex;
            ui.set("#NvEntry" + i + ".Background", isSelected ? "#44cc8830" : "#ffffff08");
            String statusDot = switch (entry.category()) {
                default -> throw new MatchException(null, null);
                case NpcViewerEntry.NpcCategory.REGISTERED -> "[R]";
                case NpcViewerEntry.NpcCategory.ORPHANED -> "[!]";
                case NpcViewerEntry.NpcCategory.WORLD -> "[W]";
            };
            ui.set("#NvStatus" + i + ".Text", statusDot);
            ui.set("#NvName" + i + ".Text", entry.getDisplayName());
            String info = switch (entry.category()) {
                default -> throw new MatchException(null, null);
                case NpcViewerEntry.NpcCategory.REGISTERED -> entry.citizenId() + " \u2014 " + entry.getPositionString();
                case NpcViewerEntry.NpcCategory.ORPHANED -> "ORPHANED \u2014 " + entry.getPositionString();
                case NpcViewerEntry.NpcCategory.WORLD -> (entry.npcTypeId() != null ? entry.npcTypeId() : "World NPC") + " \u2014 " + entry.getPositionString();
            };
            ui.set("#NvInfo" + i + ".Text", info);
        }
        int totalPages = Math.max(1, (int)Math.ceil((double)this.filteredEntries.size() / 16.0));
        ui.set("#NvPageLabel.Text", this.listPage + 1 + " / " + totalPages);
        ui.set("#NvPrevBtn.Visible", this.listPage > 0);
        ui.set("#NvNextBtn.Visible", this.listPage < totalPages - 1);
    }

    private void buildDetailPanel(UICommandBuilder ui) {
        boolean hasSelection = this.selectedIndex >= 0 && this.selectedIndex < this.filteredEntries.size();
        ui.set("#NvEmptyDetail.Visible", !hasSelection);
        ui.set("#NvDetailPanel.Visible", hasSelection);
        if (!hasSelection) {
            return;
        }
        NpcViewerEntry entry = this.filteredEntries.get(this.selectedIndex);
        CoreI18n i18n = CoreI18n.getInstance();
        ui.set("#NvDetailTitle.Text", entry.getDisplayName());
        String categoryText = switch (entry.category()) {
            default -> throw new MatchException(null, null);
            case NpcViewerEntry.NpcCategory.REGISTERED -> i18n.get("npcviewer.status.registered", entry.citizenId());
            case NpcViewerEntry.NpcCategory.ORPHANED -> i18n.get("npcviewer.status.orphaned");
            case NpcViewerEntry.NpcCategory.WORLD -> i18n.get("npcviewer.status.world");
        };
        ui.set("#NvDetailCategory.Text", categoryText);
        ui.set("#NvDetailType.Text", entry.npcTypeId() != null ? entry.npcTypeId() : "\u2014");
        ui.set("#NvDetailRole.Text", entry.roleName() != null ? entry.roleName() : "\u2014");
        ui.set("#NvDetailPos.Text", entry.getPositionString());
        ui.set("#NvDetailUuid.Text", entry.entityUUID() != null ? entry.entityUUID().toString() : "\u2014");
        boolean isRegistered = entry.category() == NpcViewerEntry.NpcCategory.REGISTERED;
        ui.set("#NvCitizenIdRow.Visible", isRegistered);
        if (isRegistered) {
            ui.set("#NvDetailCitizenId.Text", entry.citizenId());
        }
        ui.set("#NvOpenCitBtn.Visible", isRegistered);
    }

    private void buildDespawnConfirm(UICommandBuilder ui) {
        ui.set("#NvDespawnConfirm.Visible", this.showingDespawnConfirm);
        if (this.showingDespawnConfirm && this.selectedIndex >= 0 && this.selectedIndex < this.filteredEntries.size()) {
            NpcViewerEntry entry = this.filteredEntries.get(this.selectedIndex);
            CoreI18n i18n = CoreI18n.getInstance();
            ui.set("#NvDespawnConfirmText.Text", i18n.get("npcviewer.despawn_confirm_text", entry.getDisplayName()));
        }
    }

    private void handleTeleport() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.filteredEntries.size()) {
            return;
        }
        NpcViewerEntry entry = this.filteredEntries.get(this.selectedIndex);
        World world = this.player.getWorld();
        if (world == null) {
            return;
        }
        world.execute(() -> {
            try {
                Ref ref = world.getEntityRef(entry.entityUUID());
                if (ref == null || !ref.isValid()) {
                    return;
                }
                Store store = world.getEntityStore().getStore();
                TransformComponent transform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
                if (transform == null || transform.getPosition() == null) {
                    return;
                }
                Vector3d pos = transform.getPosition();
                String cmd = "tp " + this.playerRef.getUsername() + " " + pos.x + " " + pos.y + " " + pos.z;
                CommandUtils.executeAsConsole(this.player, cmd);
                CoreI18n i18n = CoreI18n.getInstance();
                this.player.sendMessage(Message.raw((String)i18n.get("npcviewer.teleported", entry.getDisplayName())).color("#88bbee"));
            }
            catch (Exception e) {
                LOGGER.warning("Failed to teleport to NPC: " + e.getMessage());
            }
        });
    }

    private void handleSummon() {
        double pz;
        double py;
        double px;
        if (this.selectedIndex < 0 || this.selectedIndex >= this.filteredEntries.size()) {
            return;
        }
        NpcViewerEntry entry = this.filteredEntries.get(this.selectedIndex);
        World world = this.player.getWorld();
        if (world == null) {
            return;
        }
        try {
            TransformComponent transform = (TransformComponent)this.storedStore.getComponent(this.storedRef, TransformComponent.getComponentType());
            Vector3d pos = transform.getPosition();
            px = pos.x;
            py = pos.y;
            pz = pos.z;
        }
        catch (Exception e) {
            LOGGER.warning("Could not get player position: " + e.getMessage());
            return;
        }
        world.execute(() -> {
            try {
                Store store;
                TransformComponent transform;
                Ref ref = world.getEntityRef(entry.entityUUID());
                if (ref != null && ref.isValid() && (transform = (TransformComponent)(store = world.getEntityStore().getStore()).getComponent(ref, TransformComponent.getComponentType())) != null) {
                    transform.teleportPosition(new Vector3d(px, py, pz));
                }
                this.allEntries = NpcViewerScanner.scan(world, this.citizenService);
                this.filterEntries();
                this.refreshUI();
            }
            catch (Exception e) {
                LOGGER.warning("Failed to summon NPC: " + e.getMessage());
            }
            CoreI18n i18n = CoreI18n.getInstance();
            this.player.sendMessage(Message.raw((String)i18n.get("npcviewer.summoned", entry.getDisplayName())).color("#88bbee"));
        });
    }

    private void handleDespawnConfirm() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.filteredEntries.size()) {
            return;
        }
        NpcViewerEntry entry = this.filteredEntries.get(this.selectedIndex);
        World world = this.player.getWorld();
        if (world == null) {
            return;
        }
        world.execute(() -> {
            try {
                if (entry.citizenId() != null) {
                    CitizenData citizen = this.citizenService.getCitizen(entry.citizenId());
                    if (citizen != null) {
                        this.citizenService.despawnCitizen(citizen, world);
                    }
                } else {
                    Ref ref = world.getEntityRef(entry.entityUUID());
                    if (ref != null && ref.isValid()) {
                        Store store = world.getEntityStore().getStore();
                        store.removeEntity(ref, RemoveReason.REMOVE);
                    }
                }
            }
            catch (Exception e) {
                LOGGER.warning("Failed to despawn NPC: " + e.getMessage());
            }
            this.showingDespawnConfirm = false;
            this.selectedIndex = -1;
            CoreI18n i18n = CoreI18n.getInstance();
            this.player.sendMessage(Message.raw((String)i18n.get("npcviewer.despawned", entry.getDisplayName())).color("#ffaa66"));
            this.allEntries = NpcViewerScanner.scan(world, this.citizenService);
            this.filterEntries();
            this.refreshUI();
        });
    }

    private void handleOpenCitizenAdmin() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.filteredEntries.size()) {
            return;
        }
        NpcViewerEntry entry = this.filteredEntries.get(this.selectedIndex);
        if (entry.citizenId() == null) {
            return;
        }
        CitizenAdminPage citizenPage = new CitizenAdminPage(this.plugin, this.player, this.playerRef);
        citizenPage.setSelectedCitizenId(entry.citizenId());
        this.player.getPageManager().openCustomPage(this.storedRef, this.storedStore, (CustomUIPage)citizenPage);
    }

    public static class PageEventData {
        public static final BuilderCodec<PageEventData> CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(PageEventData.class, PageEventData::new).addField(new KeyedCodec("Button", (Codec)Codec.STRING), (d, v) -> {
            d.button = v;
        }, d -> d.button)).addField(new KeyedCodec("@Filter", (Codec)Codec.STRING), (d, v) -> {
            d.filterValue = v;
        }, d -> d.filterValue)).addField(new KeyedCodec("@Search", (Codec)Codec.STRING), (d, v) -> {
            d.search = v;
        }, d -> d.search)).build();
        String button;
        String filterValue;
        String search;
    }
}

