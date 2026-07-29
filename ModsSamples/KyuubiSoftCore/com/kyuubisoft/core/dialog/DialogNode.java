/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.dialog;

import com.kyuubisoft.core.dialog.DialogChoice;
import com.kyuubisoft.core.dialog.DialogCondition;
import com.kyuubisoft.core.dialog.DialogMacro;
import com.kyuubisoft.core.dialog.DialogNodeType;
import java.util.List;

public class DialogNode {
    public String nodeId;
    public String type;
    public List<String> lines;
    public List<DialogChoice> choices;
    public String next;
    public boolean typewriterEffect = true;
    public DialogMacro macro;
    public DialogCondition condition;

    public DialogNodeType getType() {
        if (this.type == null) {
            return DialogNodeType.TEXT;
        }
        try {
            return DialogNodeType.valueOf(this.type.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            return DialogNodeType.TEXT;
        }
    }
}

