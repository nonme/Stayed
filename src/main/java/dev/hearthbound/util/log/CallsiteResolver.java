package dev.hearthbound.util.log;

/**
 * Resolves the file:line that called the logger, skipping our own frames.
 * Walks up the stack until it leaves the dev.hearthbound.util.log package.
 *
 * Stack walking is moderately expensive — only invoked for events that will
 * actually be printed to console (per Log.dispatch gating).
 */
final class CallsiteResolver {

    private static final String OWN_PKG = "dev.hearthbound.util.log.";

    static String resolve() {
        StackTraceElement[] frames = new Throwable().getStackTrace();
        for (StackTraceElement f : frames) {
            String cn = f.getClassName();
            if (cn.startsWith(OWN_PKG)) continue;
            return shortName(cn) + ":" + f.getLineNumber();
        }
        return null;
    }

    private static String shortName(String fqcn) {
        int dot = fqcn.lastIndexOf('.');
        return dot < 0 ? fqcn : fqcn.substring(dot + 1);
    }

    private CallsiteResolver() {}
}
