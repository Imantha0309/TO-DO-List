/*
 * Decompiled with CFR 0.152.
 */
package com.studyforge.service;

import java.util.UUID;

public class IdGen {
    public static String uid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

