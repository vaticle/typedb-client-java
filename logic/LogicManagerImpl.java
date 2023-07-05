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

package com.vaticle.typedb.client.logic;

import com.vaticle.typedb.client.api.logic.LogicManager;
import com.vaticle.typedb.client.api.logic.Rule;
import com.vaticle.typeql.lang.pattern.Pattern;

import javax.annotation.Nullable;
import java.util.stream.Stream;

import static com.vaticle.typedb.client.jni.typedb_client_jni.logic_manager_get_rule;
import static com.vaticle.typedb.client.jni.typedb_client_jni.logic_manager_get_rules;
import static com.vaticle.typedb.client.jni.typedb_client_jni.logic_manager_put_rule;

public final class LogicManagerImpl implements LogicManager {

    final com.vaticle.typedb.client.jni.Transaction transaction;

    public LogicManagerImpl(com.vaticle.typedb.client.jni.Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    @Nullable
    public Rule getRule(String label) {
        com.vaticle.typedb.client.jni.Rule res = logic_manager_get_rule(transaction, label);
        if (res != null) return new RuleImpl(res);
        else return null;
    }

    @Override
    public Stream<RuleImpl> getRules() {
        return logic_manager_get_rules(transaction).stream().map(RuleImpl::new);
    }

    @Override
    public Rule putRule(String label, Pattern when, Pattern then) {
        return new RuleImpl(logic_manager_put_rule(transaction, label, when.toString(), then.toString()));
    }
}