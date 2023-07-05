/*
 * Copyright (C) 2022 Vaticle
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.vaticle.typedb.client.concept.thing;

import com.vaticle.typedb.client.api.TypeDBTransaction;
import com.vaticle.typedb.client.api.concept.thing.Attribute;
import com.vaticle.typedb.client.api.concept.thing.Thing;
import com.vaticle.typedb.client.api.concept.type.AttributeType;
import com.vaticle.typedb.client.api.concept.type.RoleType;
import com.vaticle.typedb.client.concept.ConceptImpl;
import com.vaticle.typedb.client.concept.ConceptManagerImpl;
import com.vaticle.typedb.client.concept.type.AttributeTypeImpl;
import com.vaticle.typedb.client.concept.type.RoleTypeImpl;
import com.vaticle.typedb.client.concept.type.ThingTypeImpl;
import com.vaticle.typeql.lang.common.TypeQLToken;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import static com.vaticle.typedb.client.jni.typedb_client_jni.concept_is_attribute;
import static com.vaticle.typedb.client.jni.typedb_client_jni.concept_is_entity;
import static com.vaticle.typedb.client.jni.typedb_client_jni.concept_is_relation;
import static com.vaticle.typedb.client.jni.typedb_client_jni.thing_delete;
import static com.vaticle.typedb.client.jni.typedb_client_jni.thing_get_has;
import static com.vaticle.typedb.client.jni.typedb_client_jni.thing_get_iid;
import static com.vaticle.typedb.client.jni.typedb_client_jni.thing_get_is_inferred;
import static com.vaticle.typedb.client.jni.typedb_client_jni.thing_get_playing;
import static com.vaticle.typedb.client.jni.typedb_client_jni.thing_get_relations;
import static com.vaticle.typedb.client.jni.typedb_client_jni.thing_is_deleted;
import static com.vaticle.typedb.client.jni.typedb_client_jni.thing_set_has;
import static com.vaticle.typedb.client.jni.typedb_client_jni.thing_unset_has;

public abstract class ThingImpl extends ConceptImpl implements Thing {

    ThingImpl(com.vaticle.typedb.client.jni.Concept concept) {
        super(concept);
    }

    public static ThingImpl of(com.vaticle.typedb.client.jni.Concept concept) {
        if (concept_is_entity(concept)) return new EntityImpl(concept);
        else if (concept_is_relation(concept)) return new RelationImpl(concept);
        else if (concept_is_attribute(concept)) return new AttributeImpl(concept);
        return null; // FIXME throw
    }

    @Override
    public final String getIID() {
        return thing_get_iid(concept);
    }

    @Override
    public abstract ThingTypeImpl getType();

    @Override
    public boolean isInferred() {
        return thing_get_is_inferred(concept);
    }

    @Override
    public ThingImpl asThing() {
        return this;
    }

    @Override
    public String toString() {
        return ""; // TODO
    }

    @Override
    public final Stream<AttributeImpl> getHas(TypeDBTransaction transaction, AttributeType... attributeTypes) {
        return thing_get_has(((ConceptManagerImpl) transaction.concepts()).transaction, concept, Arrays.stream(attributeTypes).map(at -> ((AttributeTypeImpl) at).concept).toArray(com.vaticle.typedb.client.jni.Concept[]::new), new String[0]).stream().map(AttributeImpl::new);
    }

    @Override
    public final Stream<AttributeImpl> getHas(TypeDBTransaction transaction, Set<TypeQLToken.Annotation> annotations) {
        return thing_get_has(((ConceptManagerImpl) transaction.concepts()).transaction, concept, new com.vaticle.typedb.client.jni.Concept[0], annotations.stream().map(TypeQLToken.Annotation::toString).toArray(String[]::new)).stream().map(AttributeImpl::new);
    }

    @Override
    public final Stream<RelationImpl> getRelations(TypeDBTransaction transaction, RoleType... roleTypes) {
        return thing_get_relations(((ConceptManagerImpl) transaction.concepts()).transaction, concept, Arrays.stream(roleTypes).map(rt -> ((RoleTypeImpl) rt).concept).toArray(com.vaticle.typedb.client.jni.Concept[]::new)).stream().map(RelationImpl::new);
    }

    @Override
    public final Stream<RoleTypeImpl> getPlaying(TypeDBTransaction transaction) {
        return thing_get_playing(((ConceptManagerImpl) transaction.concepts()).transaction, concept).stream().map(RoleTypeImpl::new);
    }

    @Override
    public final void setHas(TypeDBTransaction transaction, Attribute attribute) {
        thing_set_has(((ConceptManagerImpl) transaction.concepts()).transaction, concept, ((AttributeImpl) attribute).concept);
    }

    @Override
    public final void unsetHas(TypeDBTransaction transaction, Attribute attribute) {
        thing_unset_has(((ConceptManagerImpl) transaction.concepts()).transaction, concept, ((AttributeImpl) attribute).concept);
    }

    @Override
    public final void delete(TypeDBTransaction transaction) {
        thing_delete(((ConceptManagerImpl) transaction.concepts()).transaction, concept);
    }

    @Override
    public final boolean isDeleted(TypeDBTransaction transaction) {
        return thing_is_deleted(((ConceptManagerImpl) transaction.concepts()).transaction, concept);
    }
}