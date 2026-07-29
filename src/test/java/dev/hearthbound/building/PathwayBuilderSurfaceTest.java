package dev.hearthbound.building;

import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;

public final class PathwayBuilderSurfaceTest {
    public static void main(String[] args) {
        gravelIsReplaceablePathSurface();
        rubbleIsClearedFromPathwayHeadroom();
        descendingStepUsesSlopedPathwayVariants();
        ascendingStepUsesSlopedPathwayVariants();
        descendingLowSideSlopeVariantsArePlacedAboveBasePathway();
        clearRestoresRecordedOriginalBlock();
        villageDataCopyKeepsPathwayOriginals();
    }

    private static void gravelIsReplaceablePathSurface() {
        assertTrue(PathwayBuilder.isReplaceablePathSurface("Soil_Grass"), "grass should be replaceable");
        assertTrue(PathwayBuilder.isReplaceablePathSurface("Soil_Grass_Full"), "full grass should be replaceable");
        assertTrue(PathwayBuilder.isReplaceablePathSurface("Soil_Gravel"), "gravel should be replaceable");
        assertFalse(PathwayBuilder.isReplaceablePathSurface("Rock_Stone"), "rock should not be replaceable");
    }

    private static void rubbleIsClearedFromPathwayHeadroom() {
        assertTrue(PathwayBuilder.isClearablePathOverlay("Rubble_Stone"), "small rubble should be cleared");
        assertTrue(PathwayBuilder.isClearablePathOverlay("Rubble_Sandstone_Medium"), "medium rubble should be cleared");
        assertTrue(PathwayBuilder.isClearablePathOverlay("Plant_Grass_Sharp"), "sharp grass should be cleared");
        assertTrue(PathwayBuilder.isClearablePathOverlay("Plant_Grass_Lust_Short"), "grass decor should be cleared");
        assertTrue(PathwayBuilder.isClearablePathOverlay("Plant_Flower_Red"), "all plant decor should be cleared");
        assertFalse(PathwayBuilder.isClearablePathOverlay("Rock_Stone"), "solid rock should not be cleared as overlay");
    }

    private static void descendingStepUsesSlopedPathwayVariants() {
        long[] high = cell(0, 0, 65);
        long[] low = cell(1, 0, 64);
        long[] lowNext = cell(2, 0, 64);

        assertEquals("Soil_Pathway_ThreeQuarter",
                PathwayBuilder.pathwayBlockForPathCell(new long[][]{high, low, lowNext}, 0),
                "high side should be three-quarter");
        assertEquals("Soil_Pathway_Half",
                PathwayBuilder.pathwayBlockForPathCell(new long[][]{high, low, lowNext}, 1),
                "low side beside step should be half");
        assertEquals("Soil_Pathway_Quarter",
                PathwayBuilder.pathwayBlockForPathCell(new long[][]{high, low, lowNext}, 2),
                "next low-side cell should be quarter");
    }

    private static void ascendingStepUsesSlopedPathwayVariants() {
        long[] lowNext = cell(0, 0, 64);
        long[] low = cell(1, 0, 64);
        long[] high = cell(2, 0, 65);

        assertEquals("Soil_Pathway_Quarter",
                PathwayBuilder.pathwayBlockForPathCell(new long[][]{lowNext, low, high}, 0),
                "outer low-side cell should be quarter");
        assertEquals("Soil_Pathway_Half",
                PathwayBuilder.pathwayBlockForPathCell(new long[][]{lowNext, low, high}, 1),
                "low side beside step should be half");
        assertEquals("Soil_Pathway_ThreeQuarter",
                PathwayBuilder.pathwayBlockForPathCell(new long[][]{lowNext, low, high}, 2),
                "high side should be three-quarter");
    }

    private static void descendingLowSideSlopeVariantsArePlacedAboveBasePathway() {
        long[] high = cell(0, 0, 65);
        long[] low = cell(1, 0, 64);
        long[] lowNext = cell(2, 0, 64);
        long[][] path = new long[][]{high, low, lowNext};

        assertEquals("Soil_Pathway_Half", PathwayBuilder.slopeOverlayBlockForPathCell(path, 1),
                "low side beside step should get half overlay");
        assertEquals(65, PathwayBuilder.slopeOverlayYForPathCell(path, 1),
                "half overlay should be placed one block above the low surface");
        assertEquals("Soil_Pathway_Quarter", PathwayBuilder.slopeOverlayBlockForPathCell(path, 2),
                "next low-side cell should get quarter overlay");
        assertEquals(65, PathwayBuilder.slopeOverlayYForPathCell(path, 2),
                "quarter overlay should be placed one block above the low surface");
        assertEquals("Soil_Pathway", PathwayBuilder.basePathwayBlockForPathCell(path, 1),
                "low side base block should remain a full pathway");
    }

    private static void clearRestoresRecordedOriginalBlock() {
        assertEquals("Soil_Gravel",
                PathwayBuilder.restoreBlockForPathwayOriginal("Soil_Gravel"),
                "clear should restore recorded gravel");
        assertEquals("Soil_Grass_Full",
                PathwayBuilder.restoreBlockForPathwayOriginal("Soil_Grass_Full"),
                "clear should restore recorded grass variant");
        assertEquals("Soil_Grass",
                PathwayBuilder.restoreBlockForPathwayOriginal(null),
                "legacy entries without original should fall back to grass");
        assertEquals("Empty",
                PathwayBuilder.restoreBlockForPathwayOriginal("Empty"),
                "clear should remove overlay blocks that were placed into empty space");
    }

    private static void villageDataCopyKeepsPathwayOriginals() {
        VillageData village = new VillageData();
        village.addPathwayBlock(1, 64, 2, "Soil_Gravel");

        VillageData copy = (VillageData) village.clone();

        assertEquals("Soil_Gravel", copy.getPathwayOriginal(0),
                "copy should preserve original pathway block ids");
    }

    private static long[] cell(int x, int z, int y) {
        return new long[]{x, z, y};
    }

    private static void assertTrue(boolean actual, String message) {
        if (!actual) throw new AssertionError(message);
    }

    private static void assertFalse(boolean actual, String message) {
        if (actual) throw new AssertionError(message);
    }

    private static void assertEquals(String expected, String actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + "\nexpected: " + expected + "\nactual:   " + actual);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + "\nexpected: " + expected + "\nactual:   " + actual);
        }
    }
}
