package dev.hearthbound.npc;

import java.nio.file.Path;

public final class HearthboundDataStoreTest {
    public static void main(String[] args) {
        saveTempFilesAreUniqueAndStayBesideDataFile();
    }

    private static void saveTempFilesAreUniqueAndStayBesideDataFile() {
        Path dataFile = Path.of("mods", "HearthboundData", "data.json");

        Path first = HearthboundDataStore.tempFileForSave(dataFile);
        Path second = HearthboundDataStore.tempFileForSave(dataFile);

        assertEquals(dataFile.getParent(), first.getParent(), "first temp parent");
        assertEquals(dataFile.getParent(), second.getParent(), "second temp parent");
        assertTrue(first.getFileName().toString().startsWith("data.json."), "first temp prefix");
        assertTrue(second.getFileName().toString().startsWith("data.json."), "second temp prefix");
        assertTrue(first.getFileName().toString().endsWith(".tmp"), "first temp suffix");
        assertTrue(second.getFileName().toString().endsWith(".tmp"), "second temp suffix");
        assertFalse(first.equals(second), "concurrent saves must not share one temp file");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean actual, String message) {
        if (!actual) throw new AssertionError(message);
    }

    private static void assertFalse(boolean actual, String message) {
        if (actual) throw new AssertionError(message);
    }
}
