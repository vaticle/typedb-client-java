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

package com.vaticle.typedb.client.user;

import com.vaticle.typedb.client.api.user.User;
import com.vaticle.typedb.client.common.NativeObject;
import com.vaticle.typedb.client.common.exception.TypeDBClientException;

import java.util.Optional;

import static com.vaticle.typedb.client.jni.typedb_client.user_get_password_expiry_seconds;
import static com.vaticle.typedb.client.jni.typedb_client.user_get_username;
import static com.vaticle.typedb.client.jni.typedb_client.user_password_update;

public class UserImpl extends NativeObject<com.vaticle.typedb.client.jni.User> implements User {
    private final UserManagerImpl users;

    UserImpl(com.vaticle.typedb.client.jni.User user, UserManagerImpl users) {
        super(user);
        this.users = users;
    }

    @Override
    public String username() {
        return user_get_username(nativeObject);
    }

    @Override
    public Optional<Long> passwordExpirySeconds() {
        var res = user_get_password_expiry_seconds(nativeObject);
        if (res >= 0) return Optional.of(res);
        else return Optional.empty();
    }

    @Override
    public void passwordUpdate(String passwordOld, String passwordNew) {
        try {
            user_password_update(nativeObject, users.nativeObject, passwordOld, passwordNew);
        } catch (com.vaticle.typedb.client.jni.Error e) {
            throw new TypeDBClientException(e);
        }
    }
}