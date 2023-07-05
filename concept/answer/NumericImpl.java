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
import com.vaticle.typedb.client.common.exception.TypeDBClientException;

import static com.vaticle.typedb.client.common.exception.ErrorMessage.Internal.ILLEGAL_CAST;
import static com.vaticle.typedb.client.jni.typedb_client_jni.numeric_get_double;
import static com.vaticle.typedb.client.jni.typedb_client_jni.numeric_get_long;
import static com.vaticle.typedb.client.jni.typedb_client_jni.numeric_is_double;
import static com.vaticle.typedb.client.jni.typedb_client_jni.numeric_is_long;
import static com.vaticle.typedb.client.jni.typedb_client_jni.numeric_is_nan;
import static com.vaticle.typedb.client.jni.typedb_client_jni.numeric_to_string;

public class NumericImpl implements Numeric {
    private final com.vaticle.typedb.client.jni.Numeric numeric;

    public NumericImpl(com.vaticle.typedb.client.jni.Numeric numeric) {
        this.numeric = numeric;
    }

    @Override
    public boolean isLong() {
        return numeric_is_long(numeric);
    }

    @Override
    public boolean isDouble() {
        return numeric_is_double(numeric);
    }

    @Override
    public boolean isNaN() {
        return numeric_is_nan(numeric);
    }

    @Override
    public long asLong() {
        if (isLong()) return numeric_get_long(numeric);
        else throw new TypeDBClientException(ILLEGAL_CAST, Long.class);
    }

    @Override
    public Double asDouble() {
        if (isDouble()) return numeric_get_double(numeric);
        else throw new TypeDBClientException(ILLEGAL_CAST, Double.class);
    }

    @Override
    public Number asNumber() {
        if (isLong()) return asLong();
        else if (isDouble()) return asDouble();
        else throw new TypeDBClientException(ILLEGAL_CAST, Number.class);
    }

    @Override
    public String toString() {
        return numeric_to_string(numeric);
    }
}