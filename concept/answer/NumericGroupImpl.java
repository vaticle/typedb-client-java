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

import com.vaticle.typedb.client.api.answer.Numeric;
import com.vaticle.typedb.client.api.answer.NumericGroup;
import com.vaticle.typedb.client.api.concept.Concept;
import com.vaticle.typedb.client.concept.ConceptImpl;

import static com.vaticle.typedb.client.jni.typedb_client.numeric_group_get_numeric;
import static com.vaticle.typedb.client.jni.typedb_client.numeric_group_get_owner;

public class NumericGroupImpl implements NumericGroup {
    private final com.vaticle.typedb.client.jni.NumericGroup numericGroup;

    public NumericGroupImpl(com.vaticle.typedb.client.jni.NumericGroup numericGroup) {
        this.numericGroup = numericGroup;
    }

    @Override
    public Concept owner() {
        return ConceptImpl.of(numeric_group_get_owner(numericGroup));
    }

    @Override
    public Numeric numeric() {
        return new NumericImpl(numeric_group_get_numeric(numericGroup));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NumericGroupImpl that = (NumericGroupImpl) obj;
        return this.numericGroup == that.numericGroup; // FIXME
    }

    @Override
    public int hashCode() {
        return numericGroup.hashCode(); // FIXME
    }
}
