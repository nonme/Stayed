/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.dialog;

import com.kyuubisoft.core.dialog.DialogNode;
import java.util.List;

public class DialogTree {
    public String id;
    public String speakerName;
    public String startNode;
    public List<DialogNode> nodes;

    public DialogNode getNode(String nodeId) {
        if (this.nodes == null || nodeId == null) {
            return null;
        }
        for (DialogNode node : this.nodes) {
            if (!nodeId.equals(node.nodeId)) continue;
            return node;
        }
        return null;
    }
}

