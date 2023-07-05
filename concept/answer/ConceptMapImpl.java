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

package com.vaticle.typedb.client.concept.answer;

import com.vaticle.typedb.client.api.answer.ConceptMap;
import com.vaticle.typedb.client.api.concept.Concept;
import com.vaticle.typedb.client.common.exception.TypeDBClientException;
import com.vaticle.typedb.client.concept.ConceptImpl;
import com.vaticle.typedb.common.collection.Pair;

import java.util.stream.Stream;

import static com.vaticle.typedb.client.common.exception.ErrorMessage.Concept.NONEXISTENT_EXPLAINABLE_CONCEPT;
import static com.vaticle.typedb.client.common.exception.ErrorMessage.Concept.NONEXISTENT_EXPLAINABLE_OWNERSHIP;
import static com.vaticle.typedb.client.common.exception.ErrorMessage.Query.VARIABLE_DOES_NOT_EXIST;
import static com.vaticle.typedb.client.jni.typedb_client_jni.concept_map_get;
import static com.vaticle.typedb.client.jni.typedb_client_jni.concept_map_get_explainables;
import static com.vaticle.typedb.client.jni.typedb_client_jni.concept_map_get_values;
import static com.vaticle.typedb.client.jni.typedb_client_jni.concept_map_get_variables;
import static com.vaticle.typedb.client.jni.typedb_client_jni.explainable_get_conjunction;
import static com.vaticle.typedb.client.jni.typedb_client_jni.explainable_get_id;
import static com.vaticle.typedb.client.jni.typedb_client_jni.explainables_get_attribute;
import static com.vaticle.typedb.client.jni.typedb_client_jni.explainables_get_attributes_keys;
import static com.vaticle.typedb.client.jni.typedb_client_jni.explainables_get_ownership;
import static com.vaticle.typedb.client.jni.typedb_client_jni.explainables_get_ownerships_keys;
import static com.vaticle.typedb.client.jni.typedb_client_jni.explainables_get_relation;
import static com.vaticle.typedb.client.jni.typedb_client_jni.explainables_get_relations_keys;

public class ConceptMapImpl implements ConceptMap {
    private final com.vaticle.typedb.client.jni.ConceptMap concept_map;

    public ConceptMapImpl(com.vaticle.typedb.client.jni.ConceptMap concept_map) {
        this.concept_map = concept_map;
    }

    @Override
    public Stream<String> variables() {
        return concept_map_get_variables(concept_map).stream();
    }

    @Override
    public Stream<Concept> concepts() {
        return concept_map_get_values(concept_map).stream().map(ConceptImpl::of);
    }

    @Override
    public Concept get(String variable) {
        com.vaticle.typedb.client.jni.Concept concept = concept_map_get(concept_map, variable);
        if (concept == null) throw new TypeDBClientException(VARIABLE_DOES_NOT_EXIST, variable);
        return ConceptImpl.of(concept);
    }

    @Override
    public Explainables explainables() {
        return new ExplainablesImpl(concept_map_get_explainables(concept_map));
    }

    @Override
    public String toString() {
        return ""; //FIXME
        //return concept_map_to_string(concept_map);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ConceptMapImpl that = (ConceptMapImpl) obj;
        return this.concept_map == that.concept_map;
        //return concept_map_equals(this.concept_map, that.concept_map);
    }

    @Override
    public int hashCode() {
        return concept_map.hashCode();
    }

    public static class ExplainablesImpl implements Explainables {

        com.vaticle.typedb.client.jni.Explainables explainables;

        ExplainablesImpl(com.vaticle.typedb.client.jni.Explainables explainables) {
            this.explainables = explainables;
        }

        @Override
        public Explainable relation(String variable) {
            com.vaticle.typedb.client.jni.Explainable explainable = explainables_get_relation(explainables, variable);
            if (explainable == null) throw new TypeDBClientException(NONEXISTENT_EXPLAINABLE_CONCEPT, variable);
            return new ExplainableImpl(explainable);
        }

        @Override
        public Explainable attribute(String variable) {
            com.vaticle.typedb.client.jni.Explainable explainable = explainables_get_attribute(explainables, variable);
            if (explainable == null) throw new TypeDBClientException(NONEXISTENT_EXPLAINABLE_CONCEPT, variable);
            return new ExplainableImpl(explainable);
        }

        @Override
        public Explainable ownership(String owner, String attribute) {
            com.vaticle.typedb.client.jni.Explainable explainable = explainables_get_ownership(explainables, owner, attribute);
            if (explainable == null)
                throw new TypeDBClientException(NONEXISTENT_EXPLAINABLE_OWNERSHIP, owner, attribute);
            return new ExplainableImpl(explainable);
        }

        @Override
        public Stream<Pair<String, Explainable>> relations() {
            return explainables_get_relations_keys(explainables).stream().map(k -> new Pair<>(k, relation(k)));
        }

        @Override
        public Stream<Pair<String, Explainable>> attributes() {
            return explainables_get_attributes_keys(explainables).stream().map(k -> new Pair<>(k, attribute(k)));
        }

        @Override
        public Stream<Pair<Pair<String, String>, Explainable>> ownerships() {
            return explainables_get_ownerships_keys(explainables).stream().map(pair -> {
                String owner = pair.get_0();
                String attribute = pair.get_1();
                return new Pair<>(new Pair<>(owner, attribute), ownership(owner, attribute));
            });
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExplainablesImpl that = (ExplainablesImpl) o;
            return this.explainables == that.explainables;  // FIXME
        }

        @Override
        public int hashCode() {
            return this.explainables.hashCode(); // FIXME
        }
    }

    static class ExplainableImpl implements Explainable {

        com.vaticle.typedb.client.jni.Explainable explainable;

        public ExplainableImpl(com.vaticle.typedb.client.jni.Explainable explainable) {
            this.explainable = explainable;
        }

        @Override
        public String conjunction() {
            return explainable_get_conjunction(explainable);
        }

        @Override
        public long id() {
            return explainable_get_id(explainable);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final ExplainableImpl that = (ExplainableImpl) o;
            return id() == that.id();
        }

        @Override
        public int hashCode() {
            return (int) id();
        }
    }
}