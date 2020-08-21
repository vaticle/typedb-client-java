/*
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

package grakn.client.connection;

import com.google.common.collect.AbstractIterator;
import com.google.protobuf.ByteString;
import grabl.tracing.client.GrablTracingThreadStatic;
import grakn.client.Grakn;
import grakn.client.Grakn.Database;
import grakn.client.Grakn.Session;
import grakn.client.Grakn.Transaction;
import grakn.client.answer.Answer;
import grakn.client.answer.AnswerGroup;
import grakn.client.answer.ConceptList;
import grakn.client.answer.ConceptMap;
import grakn.client.answer.ConceptSet;
import grakn.client.answer.ConceptSetMeasure;
import grakn.client.answer.Explanation;
import grakn.client.answer.Numeric;
import grakn.client.answer.Void;
import grakn.client.common.exception.GraknClientException;
import grakn.client.concept.thing.Thing;
import grakn.client.concept.type.AttributeType;
import grakn.client.concept.type.AttributeType.ValueType;
import grakn.client.concept.type.EntityType;
import grakn.client.concept.type.RelationType;
import grakn.client.concept.type.RoleType;
import grakn.client.concept.type.Rule;
import grakn.client.concept.type.ThingType;
import grakn.protocol.AnswerProto;
import grakn.protocol.ConceptProto;
import grakn.protocol.GraknGrpc;
import grakn.protocol.OptionsProto;
import grakn.protocol.TransactionProto;
import graql.lang.common.GraqlToken;
import graql.lang.pattern.Pattern;
import graql.lang.query.GraqlCompute;
import graql.lang.query.GraqlDefine;
import graql.lang.query.GraqlDelete;
import graql.lang.query.GraqlGet;
import graql.lang.query.GraqlInsert;
import graql.lang.query.GraqlQuery;
import graql.lang.query.GraqlUndefine;
import io.grpc.ManagedChannel;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static grabl.tracing.client.GrablTracingThreadStatic.traceOnThread;
import static grakn.client.common.exception.ErrorMessage.Connection.INVALID_BATCH_SIZE_MODE;
import static grakn.client.common.exception.ErrorMessage.Connection.NEGATIVE_BATCH_SIZE;
import static grakn.client.common.exception.ErrorMessage.Protocol.REQUIRED_FIELD_NOT_SET;
import static grakn.client.common.exception.ErrorMessage.Query.UNRECOGNISED_QUERY_OBJECT;
import static grakn.client.concept.ConceptMessageWriter.concept;
import static grakn.client.concept.ConceptMessageWriter.iid;
import static grakn.client.concept.ConceptMessageWriter.valueType;
import static grakn.client.connection.ConnectionMessageWriter.tracingData;

public class GraknTransaction implements Transaction {

    private final Session session;
    private final Type type;
    private final HashMap<String, grakn.client.concept.type.Type.Local> typeCache;
    private final GraknTransceiver transceiver;

    public static class Builder implements Transaction.Builder {

        private ManagedChannel channel;
        private Session session;
        private ByteString sessionId;

        public Builder(ManagedChannel channel, Session session, ByteString sessionId) {
            this.channel = channel;
            this.session = session;
            this.sessionId = sessionId;
        }

        @Override
        public Transaction read() {
            return new GraknTransaction(channel, session, sessionId, Type.READ);
        }

        @Override
        public Transaction write() {
            return new GraknTransaction(channel, session, sessionId, Type.WRITE);
        }
    }

    GraknTransaction(ManagedChannel channel, Session session, ByteString sessionId, Type type) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread(type == Type.WRITE ? "tx.write" : "tx.read")) {
            this.transceiver = GraknTransceiver.create(GraknGrpc.newStub(channel));
            this.session = session;
            this.type = type;
            this.typeCache = new HashMap<>();

            final TransactionProto.Transaction.Req openTxReq = TransactionProto.Transaction.Req.newBuilder()
                    .putAllMetadata(tracingData())
                    .setOpenReq(TransactionProto.Transaction.Open.Req.newBuilder()
                            .setSessionID(sessionId)
                            .setType(TransactionProto.Transaction.Type.forNumber(type.id()))).build();

            sendAndReceiveOrThrow(openTxReq);
        }
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public Session session() {
        return session;
    }

    @Override
    public Database database() {
        return session.database();
    }

    @Override
    public QueryFuture<List<ConceptMap>> execute(GraqlDefine query) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.execute.define")) {
            return executeInternal(query, Options.DEFAULT);
        }
    }

    @Override
    public QueryFuture<List<ConceptMap>> execute(GraqlUndefine query) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.execute.undefine")) {
            return executeInternal(query, Options.DEFAULT);
        }
    }

    @Override
    public QueryFuture<List<ConceptMap>> execute(GraqlInsert query, QueryOptions options) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.execute.insert")) {
            return executeInternal(query, options);
        }
    }
    @Override
    public QueryFuture<List<ConceptMap>> execute(GraqlInsert query) {
        return execute(query, Options.DEFAULT);
    }

    @Override
    public QueryFuture<List<Void>> execute(GraqlDelete query, QueryOptions options) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.execute.delete")) {
            return executeInternal(query, options);
        }
    }

    @Override
    public QueryFuture<List<Void>> execute(GraqlDelete query) {
        return execute(query, Options.DEFAULT);
    }

    @Override
    public QueryFuture<List<ConceptMap>> execute(GraqlGet query, QueryOptions options) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.execute.get")) {
            return executeInternal(query, options);
        }
    }

    @Override
    public QueryFuture<List<ConceptMap>> execute(GraqlGet query) {
        return execute(query, Options.DEFAULT);
    }

    @Override
    public QueryFuture<Stream<ConceptMap>> stream(GraqlDefine query) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.stream.define")) {
            return streamInternal(query, Options.DEFAULT);
        }
    }

    @Override
    public QueryFuture<Stream<ConceptMap>> stream(GraqlUndefine query) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.stream.undefine")) {
            return streamInternal(query, Options.DEFAULT);
        }
    }

    @Override
    public QueryFuture<Stream<ConceptMap>> stream(GraqlInsert query, QueryOptions options) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.stream.insert")) {
            return streamInternal(query, options);
        }
    }

    @Override
    public QueryFuture<Stream<ConceptMap>> stream(GraqlInsert query) {
        return stream(query, Options.DEFAULT);
    }

    @Override
    public QueryFuture<Stream<Void>> stream(GraqlDelete query, QueryOptions options) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.stream.delete")) {
            return streamInternal(query, options);
        }
    }

    @Override
    public QueryFuture<Stream<Void>> stream(GraqlDelete query) {
        return stream(query, Options.DEFAULT);
    }

    @Override
    public QueryFuture<Stream<ConceptMap>> stream(GraqlGet query, QueryOptions options) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.stream.get")) {
            return streamInternal(query, options);
        }
    }

    @Override
    public QueryFuture<Stream<ConceptMap>> stream(GraqlGet query) {
        return stream(query, Options.DEFAULT);
    }

    // Aggregate Query

    @Override
    public QueryFuture<List<Numeric>> execute(GraqlGet.Aggregate query) {
        return execute(query, Options.DEFAULT);
    }

    @Override
    public QueryFuture<List<Numeric>> execute(GraqlGet.Aggregate query, QueryOptions options) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.execute.get.aggregate")) {
            return executeInternal(query, options);
        }
    }

    @Override
    public QueryFuture<Stream<Numeric>> stream(GraqlGet.Aggregate query) {
        return stream(query, Options.DEFAULT);
    }

    @Override
    public QueryFuture<Stream<Numeric>> stream(GraqlGet.Aggregate query, QueryOptions options) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.stream.get.aggregate")) {
            return streamInternal(query, options);
        }
    }

    // Group Query

    @Override
    public QueryFuture<List<AnswerGroup<ConceptMap>>> execute(GraqlGet.Group query) {
        return execute(query, Options.DEFAULT);
    }

    @Override
    public QueryFuture<List<AnswerGroup<ConceptMap>>> execute(GraqlGet.Group query, QueryOptions options) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.execute.get.group")) {
            return executeInternal(query, options);
        }
    }

    @Override
    public QueryFuture<Stream<AnswerGroup<ConceptMap>>> stream(GraqlGet.Group query) {
        return stream(query, Options.DEFAULT);
    }

    @Override
    public QueryFuture<Stream<AnswerGroup<ConceptMap>>> stream(GraqlGet.Group query, QueryOptions options) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.stream.get.group")) {
            return streamInternal(query, options);
        }
    }

    // Group Aggregate Query

    @Override
    public QueryFuture<List<AnswerGroup<Numeric>>> execute(GraqlGet.Group.Aggregate query) {
        return execute(query, Options.DEFAULT);
    }

    @Override
    public QueryFuture<List<AnswerGroup<Numeric>>> execute(GraqlGet.Group.Aggregate query, QueryOptions options) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.execute.get.group.aggregate")) {
            return executeInternal(query, options);
        }
    }

    @Override
    public QueryFuture<Stream<AnswerGroup<Numeric>>> stream(GraqlGet.Group.Aggregate query) {
        return stream(query, Options.DEFAULT);
    }

    @Override
    public QueryFuture<Stream<AnswerGroup<Numeric>>> stream(GraqlGet.Group.Aggregate query, QueryOptions options) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.stream.get.group.aggregate")) {
            return streamInternal(query, options);
        }
    }

    // Compute Query

    @Override
    public QueryFuture<List<Numeric>> execute(GraqlCompute.Statistics query) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.execute.compute.statistics")) {
            return executeInternal(query);
        }
    }

    @Override
    public QueryFuture<Stream<Numeric>> stream(GraqlCompute.Statistics query) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.stream.compute.statistics")) {
            return streamInternal(query);
        }
    }

    @Override
    public QueryFuture<List<ConceptList>> execute(GraqlCompute.Path query) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.execute.compute.path")) {
            return executeInternal(query);
        }
    }

    @Override
    public QueryFuture<Stream<ConceptList>> stream(GraqlCompute.Path query) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.stream.compute.path")) {
            return streamInternal(query);
        }
    }

    @Override
    public QueryFuture<List<ConceptSetMeasure>> execute(GraqlCompute.Centrality query) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.execute.compute.centrality")) {
            return executeInternal(query);
        }
    }

    @Override
    public QueryFuture<Stream<ConceptSetMeasure>> stream(GraqlCompute.Centrality query) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.stream.compute.centrality")) {
            return streamInternal(query);
        }
    }

    @Override
    public QueryFuture<List<ConceptSet>> execute(GraqlCompute.Cluster query) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.execute.compute.cluster")) {
            return executeInternal(query);
        }
    }

    @Override
    public QueryFuture<Stream<ConceptSet>> stream(GraqlCompute.Cluster query) {
        try (GrablTracingThreadStatic.ThreadTrace trace = traceOnThread("tx.stream.compute.cluster")) {
            return streamInternal(query);
        }
    }

    // Generic queries

    @Override
    public QueryFuture<? extends List<? extends Answer>> execute(GraqlQuery query) {
        return execute(query, Options.DEFAULT);
    }

    @Override
    public QueryFuture<? extends List<? extends Answer>> execute(GraqlQuery query, QueryOptions options) {
        if (query instanceof GraqlDefine) {
            return execute((GraqlDefine) query);

        } else if (query instanceof GraqlUndefine) {
            return execute((GraqlUndefine) query);

        } else if (query instanceof GraqlInsert) {
            return execute((GraqlInsert) query, options);

        } else if (query instanceof GraqlDelete) {
            return execute((GraqlDelete) query, options);

        } else if (query instanceof GraqlGet) {
            return execute((GraqlGet) query, options);

        } else if (query instanceof GraqlGet.Aggregate) {
            return execute((GraqlGet.Aggregate) query, options);

        } else if (query instanceof GraqlGet.Group.Aggregate) {
            return execute((GraqlGet.Group.Aggregate) query, options);

        } else if (query instanceof GraqlGet.Group) {
            return execute((GraqlGet.Group) query, options);

        } else if (query instanceof GraqlCompute.Statistics) {
            return execute((GraqlCompute.Statistics) query);

        } else if (query instanceof GraqlCompute.Path) {
            return execute((GraqlCompute.Path) query);

        } else if (query instanceof GraqlCompute.Centrality) {
            return execute((GraqlCompute.Centrality) query);

        } else if (query instanceof GraqlCompute.Cluster) {
            return execute((GraqlCompute.Cluster) query);

        } else {
            throw new GraknClientException(UNRECOGNISED_QUERY_OBJECT.message(query));
        }
    }

    @Override
    public QueryFuture<? extends Stream<? extends Answer>> stream(GraqlQuery query) {
        return stream(query, Options.DEFAULT);
    }

    @Override
    public QueryFuture<? extends Stream<? extends Answer>> stream(GraqlQuery query, QueryOptions options) {
        if (query instanceof GraqlDefine) {
            return stream((GraqlDefine) query);

        } else if (query instanceof GraqlUndefine) {
            return stream((GraqlUndefine) query);

        } else if (query instanceof GraqlInsert) {
            return stream((GraqlInsert) query, options);

        } else if (query instanceof GraqlDelete) {
            return stream((GraqlDelete) query, options);

        } else if (query instanceof GraqlGet) {
            return stream((GraqlGet) query, options);

        } else if (query instanceof GraqlGet.Aggregate) {
            return stream((GraqlGet.Aggregate) query, options);

        } else if (query instanceof GraqlGet.Group.Aggregate) {
            return stream((GraqlGet.Group.Aggregate) query, options);

        } else if (query instanceof GraqlGet.Group) {
            return stream((GraqlGet.Group) query, options);

        } else if (query instanceof GraqlCompute.Statistics) {
            return stream((GraqlCompute.Statistics) query);

        } else if (query instanceof GraqlCompute.Path) {
            return stream((GraqlCompute.Path) query);

        } else if (query instanceof GraqlCompute.Centrality) {
            return stream((GraqlCompute.Centrality) query);

        } else if (query instanceof GraqlCompute.Cluster) {
            return stream((GraqlCompute.Cluster) query);

        } else {
            throw new GraknClientException(UNRECOGNISED_QUERY_OBJECT.message(query));
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Answer> RPCIterator<T> getQueryIterator(final GraqlQuery query, final QueryOptions options) {
        final OptionsProto.Options.Builder optionsBuilder = OptionsProto.Options.newBuilder();
        options.whenSet(Grakn.Transaction.BooleanOption.INFER, optionsBuilder::setInfer)
                .whenSet(Grakn.Transaction.BooleanOption.EXPLAIN, optionsBuilder::setExplain);

        final TransactionProto.Transaction.Iter.Req.Builder reqBuilder = TransactionProto.Transaction.Iter.Req.newBuilder()
                .setQueryIterReq(TransactionProto.Transaction.Query.Iter.Req.newBuilder()
                        .setQuery(query.toString())
                        .setOptions(optionsBuilder));

        options.whenSet(Grakn.Transaction.BatchOption.BATCH_SIZE, reqBuilder::setOptions);

        final TransactionProto.Transaction.Iter.Req iterReq = reqBuilder.build();
        return new RPCIterator<>(iterReq, response -> (T) Answer.of(this, response.getQueryIterRes().getAnswer()));
    }

    private <T extends Answer> QueryFuture<List<T>> executeInternal(GraqlQuery query) {
        return executeInternal(query, Options.DEFAULT);
    }

    private <T extends Answer> QueryFuture<List<T>> executeInternal(GraqlQuery query, QueryOptions options) {
        return new QueryExecuteFuture<>(getQueryIterator(query, options));
    }

    private <T extends Answer> QueryFuture<Stream<T>> streamInternal(GraqlQuery query) {
        return streamInternal(query, Options.DEFAULT);
    }

    private <T extends Answer> QueryFuture<Stream<T>> streamInternal(GraqlQuery query, QueryOptions options) {
        return new QueryStreamFuture<>(getQueryIterator(query, options));
    }

    public void close() {
        transceiver.close();
    }

    @Override
    public boolean isOpen() {
        return transceiver.isOpen();
    }

    private TransactionProto.Transaction.Res sendAndReceiveOrThrow(TransactionProto.Transaction.Req request) {
        try {
            return transceiver.sendAndReceive(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // This is called from classes like Transaction, that impl methods which do not throw InterruptedException
            // Therefore, we have to wrap it in a RuntimeException.
            throw new GraknClientException(e);
        }
    }

    @Override
    public void commit() {
        final TransactionProto.Transaction.Req commitReq = TransactionProto.Transaction.Req.newBuilder()
                .putAllMetadata(tracingData())
                .setCommitReq(TransactionProto.Transaction.Commit.Req.getDefaultInstance()).build();

        sendAndReceiveOrThrow(commitReq);
        close();
    }

    // TODO: is this a reasonable way of implementing this method?
    @Override
    public ThingType.Remote getRootType() {
        return getType(GraqlToken.Type.THING.toString()).asThingType();
    }

    @Override
    public EntityType.Remote getRootEntityType() {
        return getType(GraqlToken.Type.ENTITY.toString()).asEntityType();
    }

    @Override
    public RelationType.Remote getRootRelationType() {
        return getType(GraqlToken.Type.RELATION.toString()).asRelationType();
    }

    @Override
    public AttributeType.Remote getRootAttributeType() {
        return getType(GraqlToken.Type.ATTRIBUTE.toString()).asAttributeType();
    }

    @Override
    public RoleType.Remote getRootRoleType() {
        return getType(GraqlToken.Type.ROLE.toString()).asRoleType();
    }

    @Override
    public Rule.Remote getRootRule() {
        return getType(GraqlToken.Type.RULE.toString()).asRule();
    }

    @Override
    public EntityType.Remote putEntityType(final String label) {
        final TransactionProto.Transaction.Req req = TransactionProto.Transaction.Req.newBuilder()
                .putAllMetadata(tracingData())
                .setPutEntityTypeReq(TransactionProto.Transaction.PutEntityType.Req.newBuilder()
                        .setLabel(label)).build();

        final TransactionProto.Transaction.Res res = sendAndReceiveOrThrow(req);
        return grakn.client.concept.type.Type.Remote.of(this, res.getPutEntityTypeRes().getEntityType()).asEntityType();
    }

    @Override
    @Nullable
    public EntityType.Remote getEntityType(final String label) {
        final grakn.client.concept.type.Type.Remote concept = getType(label);
        if (concept instanceof ThingType.Remote) {
            return (grakn.client.concept.type.EntityType.Remote) concept;
        } else {
            return null;
        }
    }

    @Override
    public RelationType.Remote putRelationType(final String label) {
        final TransactionProto.Transaction.Req req = TransactionProto.Transaction.Req.newBuilder()
                .putAllMetadata(tracingData())
                .setPutRelationTypeReq(TransactionProto.Transaction.PutRelationType.Req.newBuilder()
                        .setLabel(label)).build();

        final TransactionProto.Transaction.Res res = sendAndReceiveOrThrow(req);
        return grakn.client.concept.type.Type.Remote.of(this, res.getPutRelationTypeRes().getRelationType()).asRelationType();
    }

    @Override
    @Nullable
    public RelationType.Remote getRelationType(final String label) {
        final grakn.client.concept.type.Type.Remote concept = getType(label);
        if (concept instanceof RelationType.Remote) {
            return (RelationType.Remote) concept;
        } else {
            return null;
        }
    }

    @Override
    public AttributeType.Remote putAttributeType(final String label, final ValueType valueType) {
        final TransactionProto.Transaction.Req req = TransactionProto.Transaction.Req.newBuilder()
                .putAllMetadata(tracingData())
                .setPutAttributeTypeReq(TransactionProto.Transaction.PutAttributeType.Req.newBuilder()
                        .setLabel(label)
                        .setValueType(valueType(valueType))).build();

        final TransactionProto.Transaction.Res res = sendAndReceiveOrThrow(req);
        return grakn.client.concept.type.Type.Remote.of(this, res.getPutAttributeTypeRes().getAttributeType()).asAttributeType();
    }

    @Override
    @Nullable
    public AttributeType.Remote getAttributeType(final String label) {
        final grakn.client.concept.type.Type.Remote concept = getType(label);
        if (concept instanceof AttributeType.Remote) {
            return (AttributeType.Remote) concept;
        } else {
            return null;
        }
    }

    @Override
    public Rule.Remote putRule(final String label, final Pattern when, final Pattern then) {
        throw new GraknClientException(new UnsupportedOperationException());
        /*final TransactionProto.Transaction.Req req = TransactionProto.Transaction.Req.newBuilder()
                .putAllMetadata(tracingData())
                .setPutRuleReq(TransactionProto.Transaction.PutRule.Req.newBuilder()
                        .setLabel(label)
                        .setWhen(when.toString())
                        .setThen(then.toString())).build();

        final TransactionProto.Transaction.Res res = sendAndReceiveOrThrow(req);
        return grakn.client.concept.type.Type.Remote.of(this, res.getPutRuleRes().getRule()).asRule();*/
    }

    @Override
    @Nullable
    public Rule.Remote getRule(String label) {
        grakn.client.concept.type.Type.Remote concept = getType(label);
        if (concept instanceof Rule.Remote) {
            return (Rule.Remote) concept;
        } else {
            return null;
        }
    }

    @Override
    @Nullable
    public grakn.client.concept.type.Type.Remote getType(final String label) {
        final TransactionProto.Transaction.Req req = TransactionProto.Transaction.Req.newBuilder()
                .putAllMetadata(tracingData())
                .setGetTypeReq(TransactionProto.Transaction.GetType.Req.newBuilder().setLabel(label)).build();

        final TransactionProto.Transaction.Res response = sendAndReceiveOrThrow(req);
        switch (response.getGetTypeRes().getResCase()) {
            case TYPE:
                final grakn.client.concept.type.Type.Remote type = grakn.client.concept.type.Type.Remote.of(this, response.getGetTypeRes().getType());
                typeCache.put(type.getLabel(), grakn.client.concept.type.Type.Local.of(response.getGetTypeRes().getType()));
                // TODO: maybe we should return the cached Type.Local? It has more information
                return type;
            default:
            case RES_NOT_SET:
                return null;
        }
    }

    @Nullable
    public grakn.client.concept.type.Type.Local getCachedType(final String label) {
        return typeCache.get(label);
    }

    @Override
    @Nullable
    public Thing.Remote getThing(final String iid) {
        final TransactionProto.Transaction.Req req = TransactionProto.Transaction.Req.newBuilder()
                .putAllMetadata(tracingData())
                .setGetThingReq(TransactionProto.Transaction.GetThing.Req.newBuilder().setIid(iid(iid))).build();

        final TransactionProto.Transaction.Res response = sendAndReceiveOrThrow(req);
        switch (response.getGetThingRes().getResCase()) {
            case THING:
                return Thing.Remote.of(this, response.getGetThingRes().getThing());
            default:
            case RES_NOT_SET:
                return null;
        }
    }

    @Override
    public TransactionProto.Transaction.Res runConceptMethod(final String iid, final ConceptProto.ThingMethod.Req thingMethod) {
        final TransactionProto.Transaction.Req request = TransactionProto.Transaction.Req.newBuilder()
                .setConceptMethodThingReq(TransactionProto.Transaction.ConceptMethod.Thing.Req.newBuilder()
                        .setIid(iid(iid))
                        .setMethod(thingMethod)).build();

        return sendAndReceiveOrThrow(request);
    }

    @Override
    public TransactionProto.Transaction.Res runConceptMethod(final String label, final ConceptProto.TypeMethod.Req typeMethod) {
        final TransactionProto.Transaction.Req request = TransactionProto.Transaction.Req.newBuilder()
                .setConceptMethodTypeReq(TransactionProto.Transaction.ConceptMethod.Type.Req.newBuilder()
                        .setLabel(label)
                        .setMethod(typeMethod)).build();

        return sendAndReceiveOrThrow(request);
    }

    @Override
    public <T> Stream<T> iterateConceptMethod(final String iid, final ConceptProto.ThingMethod.Iter.Req method, final Function<ConceptProto.ThingMethod.Iter.Res, T> responseReader) {
        final TransactionProto.Transaction.Iter.Req request = TransactionProto.Transaction.Iter.Req.newBuilder()
                .setConceptMethodThingIterReq(TransactionProto.Transaction.ConceptMethod.Thing.Iter.Req.newBuilder()
                        .setIid(iid(iid))
                        .setMethod(method)).build();

        return iterate(request, res -> responseReader.apply(res.getConceptMethodThingIterRes().getResponse()));
    }

    @Override
    public <T> Stream<T> iterateConceptMethod(final String label, final ConceptProto.TypeMethod.Iter.Req method, final Function<ConceptProto.TypeMethod.Iter.Res, T> responseReader) {
        final TransactionProto.Transaction.Iter.Req request = TransactionProto.Transaction.Iter.Req.newBuilder()
                .setConceptMethodTypeIterReq(TransactionProto.Transaction.ConceptMethod.Type.Iter.Req.newBuilder()
                        .setLabel(label)
                        .setMethod(method)).build();

        return iterate(request, res -> responseReader.apply(res.getConceptMethodTypeIterRes().getResponse()));
    }

    @Override
    public Explanation getExplanation(ConceptMap explainable) {
        AnswerProto.ConceptMap conceptMapProto = conceptMap(explainable);
        AnswerProto.Explanation.Req explanationReq = AnswerProto.Explanation.Req.newBuilder().setExplainable(conceptMapProto).build();
        TransactionProto.Transaction.Req request = TransactionProto.Transaction.Req.newBuilder().setExplanationReq(explanationReq).build();
        TransactionProto.Transaction.Res response = sendAndReceiveOrThrow(request);
        return Explanation.of(this, response.getExplanationRes());
    }

    private AnswerProto.ConceptMap conceptMap(ConceptMap conceptMap) {
        AnswerProto.ConceptMap.Builder conceptMapProto = AnswerProto.ConceptMap.newBuilder();
        conceptMap.map().forEach((var, concept) -> {
            ConceptProto.Concept conceptProto = concept(concept);
            conceptMapProto.putMap(var, conceptProto);
        });
        conceptMapProto.setHasExplanation(conceptMap.hasExplanation());
        conceptMapProto.setPattern(conceptMap.queryPattern().toString());
        return conceptMapProto.build();
    }

    @Override
    public <T> Stream<T> iterate(TransactionProto.Transaction.Iter.Req request, Function<TransactionProto.Transaction.Iter.Res, T> responseReader) {
        return StreamSupport.stream(((Iterable<T>) () -> new RPCIterator<>(request, responseReader)).spliterator(), false);
    }

    private abstract class QueryFutureBase<T> implements QueryFuture<T> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false; // Can't cancel
        }

        @Override
        public boolean isCancelled() {
            return false; // Can't cancel
        }

        @Override
        public boolean isDone() {
            return getIterator().isStarted();
        }

        @Override
        public T get() {
            getIterator().waitForStart();
            return getInternal();
        }

        @Override
        public T get(long timeout, TimeUnit unit) {
            try {
                getIterator().waitForStart(timeout, unit);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new GraknClientException(ex);
            } catch (TimeoutException ex) {
                throw new GraknClientException(ex);
            }
            return getInternal();
        }

        protected abstract RPCIterator<?> getIterator();
        protected abstract T getInternal();
    }

    private class QueryStreamFuture<T> extends QueryFutureBase<Stream<T>> {
        private RPCIterator<T> iterator;

        protected QueryStreamFuture(RPCIterator<T> iterator) {
            this.iterator = iterator;
        }

        @Override
        protected RPCIterator<?> getIterator() {
            return iterator;
        }

        @Override
        protected Stream<T> getInternal() {
            return StreamSupport.stream(((Iterable<T>) () -> iterator).spliterator(), false);
        }
    }

    private class QueryExecuteFuture<T> extends QueryFutureBase<List<T>> {
        private RPCIterator<T> iterator;

        protected QueryExecuteFuture(RPCIterator<T> iterator) {
            this.iterator = iterator;
        }

        @Override
        protected RPCIterator<?> getIterator() {
            return iterator;
        }

        @Override
        protected List<T> getInternal() {
            List<T> result = new ArrayList<>();
            iterator.forEachRemaining(result::add);
            return result;
        }
    }

    /**
     * A client-side iterator over gRPC messages. Will send TransactionProto.Transaction.Iter.Req messages until
     * TransactionProto.Transaction.Iter.Res returns done as a message.
     *
     * @param <T> class type of objects being iterated
     */
    public class RPCIterator<T> extends AbstractIterator<T> {
        private Function<TransactionProto.Transaction.Iter.Res, T> responseReader;
        private Batch currentBatch;
        private volatile boolean started;
        private TransactionProto.Transaction.Iter.Res first;
        private TransactionProto.Transaction.Iter.Req.Options options;

        private RPCIterator(TransactionProto.Transaction.Iter.Req req,
                            Function<TransactionProto.Transaction.Iter.Res, T> responseReader) {
            this.responseReader = responseReader;
            options = req.getOptions();
            sendRequest(req);
        }

        private void sendRequest(TransactionProto.Transaction.Iter.Req req) {
            currentBatch = new Batch();

            TransactionProto.Transaction.Req transactionReq = TransactionProto.Transaction.Req.newBuilder()
                    .setIterReq(req).build();

            transceiver.sendAndReceiveMultipleAsync(transactionReq, currentBatch);
        }

        private void nextBatch(int iteratorID) {
            TransactionProto.Transaction.Iter.Req iterReq = TransactionProto.Transaction.Iter.Req.newBuilder()
                    .setIteratorID(iteratorID)
                    .setOptions(options)
                    .build();

            sendRequest(iterReq);
        }

        private class Batch extends GraknTransceiver.MultiResponseCollector {
            @Override
            protected boolean isLastResponse(TransactionProto.Transaction.Res response) {
                TransactionProto.Transaction.Iter.Res iterRes = response.getIterRes();
                return iterRes.getIteratorID() != 0 || iterRes.getDone();
            }
        }

        public boolean isStarted() {
            return started;
        }

        public void waitForStart(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            if (first != null) {
                throw new GraknClientException(new IllegalStateException("Should not poll RPCIterator multiple times"));
            }

            first = currentBatch.poll(timeout, unit).getIterRes();
        }

        public void waitForStart() {
        }

        @Override
        protected T computeNext() {
            if (first != null) {
                TransactionProto.Transaction.Iter.Res iterRes = first;
                first = null;
                return responseReader.apply(iterRes);
            }

            final TransactionProto.Transaction.Iter.Res res;
            try {
                res = currentBatch.take().getIterRes();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GraknClientException(e);
            }
            started = true;
            switch (res.getResCase()) {
                case ITERATORID:
                    nextBatch(res.getIteratorID());
                    return computeNext();
                case DONE:
                    return endOfData();
                case RES_NOT_SET:
                    throw new GraknClientException(REQUIRED_FIELD_NOT_SET.message(TransactionProto.Transaction.Iter.Res.class.getCanonicalName()));
                default:
                    return responseReader.apply(res);
            }
        }
    }

    public static class QueryOptionsImpl implements QueryOptions {
        private Map<Option<?>, Object> options;

        public QueryOptionsImpl() {
            options = new HashMap<>();
        }

        public QueryOptionsImpl(Map<Option<?>, Object> options) {
            this.options = options;
        }

        @Override
        public <T> QueryOptions set(Option<T> option, T value) {
            Map<Option<?>, Object> cloned = new HashMap<>(options);
            cloned.put(option, value);
            return new QueryOptionsImpl(cloned);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> QueryOptions whenSet(Option<T> option, Consumer<T> consumer) {
            T value = (T) options.get(option);
            if (value != null) {
                consumer.accept(value);
            }
            return this;
        }

        @Override
        public QueryOptions infer(boolean infer) {
            return set(BooleanOption.INFER, infer);
        }

        @Override
        public QueryOptions explain(boolean explain) {
            return set(BooleanOption.EXPLAIN, explain);
        }

        @Override
        public QueryOptions batchSize(int size) {
            if (size < 1) {
                throw new GraknClientException(NEGATIVE_BATCH_SIZE.message(size));
            }
            return set(BatchOption.BATCH_SIZE, TransactionProto.Transaction.Iter.Req.Options.newBuilder().setNumber(size).build());
        }

        @Override
        public QueryOptions batchSize(BatchSize batchSize) {
            if (batchSize == BatchSize.ALL) {
                return set(BatchOption.BATCH_SIZE, TransactionProto.Transaction.Iter.Req.Options.newBuilder().setAll(true).build());
            }
            throw new GraknClientException(INVALID_BATCH_SIZE_MODE.message(batchSize));
        }
    }
}
