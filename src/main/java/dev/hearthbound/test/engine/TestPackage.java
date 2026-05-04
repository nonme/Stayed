package dev.hearthbound.test.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Named bundle of {@link TestCase}s. {@code /hb test pkg <name>} runs every
 * case in order. Cases inside a package share state with the same village —
 * cleanup happens through CleanupTestNpcsStep at each case's tail rather than
 * a full village wipe.
 */
public final class TestPackage {

    private final String name;
    private final List<TestCase> cases;

    public TestPackage(String name, List<TestCase> cases) {
        this.name = name;
        this.cases = Collections.unmodifiableList(new ArrayList<>(cases));
    }

    public String getName() { return name; }
    public List<TestCase> getCases() { return cases; }
}
