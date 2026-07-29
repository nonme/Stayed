/*
 * Decompiled with CFR 0.152.
 */
package com.yourname.companion;

import java.io.InputStream;
import java.util.Properties;

public final class BuildInfo {
    private static final String UNKNOWN = "unknown";
    private static final String buildId;
    private static final String version;
    private static final String git;
    private static final String timestampUtc;

    private BuildInfo() {
    }

    public static String buildId() {
        return buildId;
    }

    public static String version() {
        return version;
    }

    public static String git() {
        return git;
    }

    public static String timestampUtc() {
        return timestampUtc;
    }

    public static String shortBuildLabel() {
        return version + " | " + git + " | " + timestampUtc;
    }

    static {
        Properties props = new Properties();
        try (InputStream in = BuildInfo.class.getClassLoader().getResourceAsStream("build-info.properties");){
            if (in != null) {
                props.load(in);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        buildId = props.getProperty("build.id", UNKNOWN);
        version = props.getProperty("build.version", UNKNOWN);
        git = props.getProperty("build.git", UNKNOWN);
        timestampUtc = props.getProperty("build.timestamp.utc", UNKNOWN);
    }
}

