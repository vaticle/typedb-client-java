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

import com.google.protobuf.ByteString;
import com.vaticle.typedb.client.api.TypeDBClient;
import com.vaticle.typedb.client.api.TypeDBOptions;
import com.vaticle.typedb.client.api.TypeDBSession;
import com.vaticle.typedb.client.common.exception.TypeDBClientException;
import com.vaticle.typedb.client.common.rpc.TypeDBStub;
import com.vaticle.typedb.client.stream.RequestTransmitter;
import com.vaticle.typedb.common.concurrent.NamedThreadFactory;
import io.grpc.ManagedChannel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static com.vaticle.typedb.client.common.exception.ErrorMessage.Client.CLIENT_NOT_OPEN;
import static com.vaticle.typedb.client.common.exception.ErrorMessage.Internal.ILLEGAL_CAST;
import static com.vaticle.typedb.common.util.Objects.className;

public abstract class TypeDBClientImpl implements TypeDBClient {

    private static final String TYPEDB_CLIENT_RPC_THREAD_NAME = "typedb-client-rpc";

    private final RequestTransmitter transmitter;
    private final TypeDBDatabaseManagerImpl databaseMgr;
    private final ConcurrentMap<ByteString, TypeDBSessionImpl> sessions;
    protected boolean isOpen;

    protected TypeDBClientImpl(int parallelisation) {
        NamedThreadFactory threadFactory = NamedThreadFactory.create(TYPEDB_CLIENT_RPC_THREAD_NAME);
        transmitter = new RequestTransmitter(parallelisation, threadFactory);
        databaseMgr = new TypeDBDatabaseManagerImpl(this);
        sessions = new ConcurrentHashMap<>();
        this.isOpen = false;
    }

    protected abstract void open();

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    public static int calculateParallelisation() {
        int cores = Runtime.getRuntime().availableProcessors();
        if (cores <= 4) return 2;
        else if (cores <= 9) return 3;
        else if (cores <= 16) return 4;
        else return (int) Math.ceil(cores / 4.0);
    }

    @Override
    public TypeDBSessionImpl session(String database, TypeDBSession.Type type) {
        return session(database, type, TypeDBOptions.core());
    }

    @Override
    public TypeDBSessionImpl session(String database, TypeDBSession.Type type, TypeDBOptions options) {
        if (!isOpen()) throw new TypeDBClientException(CLIENT_NOT_OPEN);
        TypeDBSessionImpl session = new TypeDBSessionImpl(this, database, type, options);
        assert !sessions.containsKey(session.id());
        sessions.put(session.id(), session);
        return session;
    }

    @Override
    public TypeDBDatabaseManagerImpl databases() {
        return databaseMgr;
    }

    @Override
    public boolean isCluster() {
        return false;
    }

    @Override
    public Cluster asCluster() {
        throw new TypeDBClientException(ILLEGAL_CAST, className(TypeDBClient.Cluster.class));
    }

    public abstract ManagedChannel channel();

    public abstract TypeDBStub stub();

    RequestTransmitter transmitter() {
        return transmitter;
    }

    void removeSession(TypeDBSessionImpl session) {
        sessions.remove(session.id());
    }

    @Override
    public void close() {
        if (isOpen) {
            isOpen = false;
            try {
                sessions.values().forEach(TypeDBSessionImpl::close);
                channel().shutdown().awaitTermination(10, TimeUnit.SECONDS);
                transmitter.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
