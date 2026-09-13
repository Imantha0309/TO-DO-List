/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.mongodb.repository.MongoRepository
 */
package com.studyforge.repository;

import com.studyforge.model.Target;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TargetRepository
extends MongoRepository<Target, String> {
    public List<Target> findByOwnerIdOrderByUpdatedAtDesc(String var1);

    public List<Target> findByOwnerId(String var1);

    public List<Target> findByCollaboratorIdsContaining(String var1);
}

