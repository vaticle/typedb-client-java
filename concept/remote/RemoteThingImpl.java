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

package grakn.client.concept.remote;

import grakn.client.GraknClient;
import grakn.client.concept.Attribute;
import grakn.client.concept.AttributeType;
import grakn.client.concept.ConceptId;
import grakn.client.concept.Role;
import grakn.client.rpc.RequestBuilder;
import grakn.protocol.session.ConceptProto;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * Client implementation of Thing
 *
 * @param <SomeRemoteThing> The exact type of this class
 * @param <SomeRemoteType>  the type of an instance of this class
 */
public abstract class RemoteThingImpl<
        SomeRemoteThing extends RemoteThing<SomeRemoteThing, SomeRemoteType>,
        SomeRemoteType extends RemoteType<SomeRemoteType, SomeRemoteThing>>
        extends RemoteConceptImpl<SomeRemoteThing>
        implements RemoteThing<SomeRemoteThing, SomeRemoteType> {

    RemoteThingImpl(GraknClient.Transaction tx, ConceptId id) {
        super(tx, id);
    }

    @Override
    public final SomeRemoteType type() {
        ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                .setThingTypeReq(ConceptProto.Thing.Type.Req.getDefaultInstance()).build();

        SomeRemoteType concept = RemoteConcept.of(runMethod(method).getThingTypeRes().getType(), tx());
        return asCurrentType(concept);
    }

    @Override
    public final boolean isInferred() {
        ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                .setThingIsInferredReq(ConceptProto.Thing.IsInferred.Req.getDefaultInstance()).build();

        return runMethod(method).getThingIsInferredRes().getInferred();
    }

    @Override
    public final Stream<RemoteAttribute<?>> keys(AttributeType<?, ?, ?>... attributeTypes) {
        ConceptProto.Method.Iter.Req method = ConceptProto.Method.Iter.Req.newBuilder()
                .setThingKeysIterReq(ConceptProto.Thing.Keys.Iter.Req.newBuilder()
                                         .addAllAttributeTypes(RequestBuilder.ConceptMessage.concepts(Arrays.asList(attributeTypes)))).build();

        return conceptStream(method, res -> res.getThingKeysIterRes().getAttribute()).map(RemoteConcept::asAttribute);
    }

    @Override
    public final <T> Stream<RemoteAttribute<T>> keys(AttributeType<T, ?, ?> attributeType) {
        ConceptProto.Method.Iter.Req method = ConceptProto.Method.Iter.Req.newBuilder()
                .setThingKeysIterReq(ConceptProto.Thing.Keys.Iter.Req.newBuilder()
                        .addAllAttributeTypes(RequestBuilder.ConceptMessage.concepts(Collections.singleton(attributeType)))).build();

        return conceptStream(method, res -> res.getThingKeysIterRes().getAttribute());
    }

    @Override
    public final Stream<RemoteAttribute<?>> attributes(AttributeType<?, ?, ?>... attributeTypes) {
        ConceptProto.Method.Iter.Req method = ConceptProto.Method.Iter.Req.newBuilder()
                .setThingAttributesIterReq(ConceptProto.Thing.Attributes.Iter.Req.newBuilder()
                                               .addAllAttributeTypes(RequestBuilder.ConceptMessage.concepts(Arrays.asList(attributeTypes)))).build();

        return conceptStream(method, res -> res.getThingAttributesIterRes().getAttribute()).map(RemoteConcept::asAttribute);
    }

    @Override
    public final <T> Stream<RemoteAttribute<T>> attributes(AttributeType<T, ?, ?> attributeType) {
        ConceptProto.Method.Iter.Req method = ConceptProto.Method.Iter.Req.newBuilder()
                .setThingAttributesIterReq(ConceptProto.Thing.Attributes.Iter.Req.newBuilder()
                        .addAllAttributeTypes(RequestBuilder.ConceptMessage.concepts(Collections.singleton(attributeType)))).build();

        return conceptStream(method, res -> res.getThingAttributesIterRes().getAttribute());
    }

    @Override
    public final Stream<RemoteRelation> relations(Role<?>... roles) {
        ConceptProto.Method.Iter.Req method = ConceptProto.Method.Iter.Req.newBuilder()
                .setThingRelationsIterReq(ConceptProto.Thing.Relations.Iter.Req.newBuilder()
                                              .addAllRoles(RequestBuilder.ConceptMessage.concepts(Arrays.asList(roles)))).build();

        return conceptStream(method, res -> res.getThingRelationsIterRes().getRelation()).map(RemoteConcept::asRelation);
    }

    @Override
    public final Stream<RemoteRole> roles() {
        ConceptProto.Method.Iter.Req method = ConceptProto.Method.Iter.Req.newBuilder()
                .setThingRolesIterReq(ConceptProto.Thing.Roles.Iter.Req.getDefaultInstance()).build();

        return conceptStream(method, res -> res.getThingRolesIterRes().getRole()).map(RemoteConcept::asRole);
    }

    @Override
    public final SomeRemoteThing has(Attribute<?, ?, ?> attribute) {
        relhas(attribute);
        return asCurrentBaseType(this);
    }

    @Deprecated
    public final RemoteRelation relhas(Attribute<?, ?, ?> attribute) {
        // TODO: replace usage of this method as a getter, with relations(Attribute attribute)
        // TODO: then remove this method altogether and just use has(Attribute attribute)
        ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                .setThingRelhasReq(ConceptProto.Thing.Relhas.Req.newBuilder()
                                           .setAttribute(RequestBuilder.ConceptMessage.from(attribute))).build();

        return RemoteConcept.of(runMethod(method).getThingRelhasRes().getRelation(), tx()).asRelation();
    }

    @Override
    public final SomeRemoteThing unhas(Attribute<?, ?, ?> attribute) {
        ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                .setThingUnhasReq(ConceptProto.Thing.Unhas.Req.newBuilder()
                                          .setAttribute(RequestBuilder.ConceptMessage.from(attribute))).build();

        runMethod(method);
        return asCurrentBaseType(this);
    }

    abstract SomeRemoteType asCurrentType(RemoteConcept<?> concept);
}
