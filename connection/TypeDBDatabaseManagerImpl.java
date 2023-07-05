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

package com.vaticle.typedb.client.connection;

import com.vaticle.typedb.client.api.database.Database;
import com.vaticle.typedb.client.api.database.DatabaseManager;

import java.util.List;

import static com.vaticle.typedb.client.jni.typedb_client_jni.database_manager_new;
import static com.vaticle.typedb.client.jni.typedb_client_jni.databases_all;
import static com.vaticle.typedb.client.jni.typedb_client_jni.databases_contains;
import static com.vaticle.typedb.client.jni.typedb_client_jni.databases_create;
import static com.vaticle.typedb.client.jni.typedb_client_jni.databases_get;
import static java.util.stream.Collectors.toList;

public class TypeDBDatabaseManagerImpl implements DatabaseManager {

    private final com.vaticle.typedb.client.jni.DatabaseManager databaseManager;

    public TypeDBDatabaseManagerImpl(com.vaticle.typedb.client.jni.Connection connection) {
        databaseManager = database_manager_new(connection);
    }

    @Override
    public Database get(String name) throws Error {
        return new TypeDBDatabaseImpl(databases_get(databaseManager, name));
    }

    @Override
    public boolean contains(String name) throws Error {
        return databases_contains(databaseManager, name);
    }

    @Override
    public void create(String name) throws Error {
        databases_create(databaseManager, name);
    }

    @Override
    public List<Database> all() {
        return databases_all(databaseManager).stream().map(TypeDBDatabaseImpl::new).collect(toList());
    }
}