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

#include "typedb/common/exception.hpp"
#include "typedb/common/native.hpp"

#include "inc/conceptfactory.hpp"
#include "inc/conceptresultwrapper.hpp"
#include "inc/macros.hpp"
#include "typedb/concept/templatespecialisation.hpp"

namespace TypeDB {

// ConceptFuture
template <typename T>
std::unique_ptr<T> resolveConceptPtrPromise(ConceptFutureWrapper* wrapper) {
    if (wrapper != nullptr) {
        auto p = wrapper->resolve();
        if (p == nullptr) return std::unique_ptr<T>();  // nullptr
        if constexpr (std::is_same_v<T, Type>) return ConceptFactory::type(p);
        else if constexpr (std::is_same_v<T, RoleType>) return ConceptFactory::roleType(p);

        else if constexpr (std::is_same_v<T, ThingType>) return ConceptFactory::thingType(p);
        else if constexpr (std::is_same_v<T, AttributeType>) return ConceptFactory::attributeType(p);
        else if constexpr (std::is_same_v<T, EntityType>) return ConceptFactory::entityType(p);
        else if constexpr (std::is_same_v<T, RelationType>) return ConceptFactory::relationType(p);

        else if constexpr (std::is_same_v<T, Thing>) return ConceptFactory::thing(p);
        else if constexpr (std::is_same_v<T, Attribute>) return ConceptFactory::attribute(p);
        else if constexpr (std::is_same_v<T, Entity>) return ConceptFactory::entity(p);
        else if constexpr (std::is_same_v<T, Relation>) return ConceptFactory::relation(p);

        else if constexpr (std::is_same_v<T, Value>) return ConceptFactory::value(p);
        else if constexpr (std::is_same_v<T, Concept>) return ConceptFactory::ofNative(p);
        else {
            static_assert(std::is_same_v<T, Concept>);  // Fail to compile with a nice message
        }

    } else {
        throw TypeDBDriverException::of(&InternalError::INVALID_NATIVE_HANDLE);
    }
}

template <>
std::function<std::unique_ptr<Type>(ConceptFutureWrapper*)> ConceptPtrFuture<Type>::fn_nativePromiseResolve = resolveConceptPtrPromise<Type>;

template <>
std::function<std::unique_ptr<RoleType>(ConceptFutureWrapper*)> ConceptPtrFuture<RoleType>::fn_nativePromiseResolve = resolveConceptPtrPromise<RoleType>;

template <>
std::function<std::unique_ptr<ThingType>(ConceptFutureWrapper*)> ConceptPtrFuture<ThingType>::fn_nativePromiseResolve = resolveConceptPtrPromise<ThingType>;

template <>
std::function<std::unique_ptr<EntityType>(ConceptFutureWrapper*)> ConceptPtrFuture<EntityType>::fn_nativePromiseResolve = resolveConceptPtrPromise<EntityType>;
template <>
std::function<std::unique_ptr<AttributeType>(ConceptFutureWrapper*)> ConceptPtrFuture<AttributeType>::fn_nativePromiseResolve = resolveConceptPtrPromise<AttributeType>;
template <>
std::function<std::unique_ptr<RelationType>(ConceptFutureWrapper*)> ConceptPtrFuture<RelationType>::fn_nativePromiseResolve = resolveConceptPtrPromise<RelationType>;

template <>
std::function<std::unique_ptr<Thing>(ConceptFutureWrapper*)> ConceptPtrFuture<Thing>::fn_nativePromiseResolve = resolveConceptPtrPromise<Thing>;

template <>
std::function<std::unique_ptr<Entity>(ConceptFutureWrapper*)> ConceptPtrFuture<Entity>::fn_nativePromiseResolve = resolveConceptPtrPromise<Entity>;
template <>
std::function<std::unique_ptr<Attribute>(ConceptFutureWrapper*)> ConceptPtrFuture<Attribute>::fn_nativePromiseResolve = resolveConceptPtrPromise<Attribute>;
template <>
std::function<std::unique_ptr<Relation>(ConceptFutureWrapper*)> ConceptPtrFuture<Relation>::fn_nativePromiseResolve = resolveConceptPtrPromise<Relation>;


// ConceptIterator Helper
void conceptIteratorWrapperDrop(ConceptIteratorWrapper* it) {
    delete it;
}

_native::Concept* conceptIteratorWrapperNext(ConceptIteratorWrapper* it) {
    return it->next();
}

#define CONCEPT_ITERATOR_HELPER(T, CONCEPT_FACTORY_METHOD) \
    TYPEDB_ITERATOR_HELPER_1(ConceptIteratorWrapper, _native::Concept, std::unique_ptr<T>, conceptIteratorWrapperDrop, conceptIteratorWrapperNext, _native::concept_drop, CONCEPT_FACTORY_METHOD)

CONCEPT_ITERATOR_HELPER(Concept, ConceptFactory::ofNative);
CONCEPT_ITERATOR_HELPER(Type, ConceptFactory::type);
CONCEPT_ITERATOR_HELPER(RoleType, ConceptFactory::roleType);

CONCEPT_ITERATOR_HELPER(ThingType, ConceptFactory::thingType);
CONCEPT_ITERATOR_HELPER(AttributeType, ConceptFactory::attributeType);
CONCEPT_ITERATOR_HELPER(EntityType, ConceptFactory::entityType);
CONCEPT_ITERATOR_HELPER(RelationType, ConceptFactory::relationType);

CONCEPT_ITERATOR_HELPER(Thing, ConceptFactory::thing);
CONCEPT_ITERATOR_HELPER(Attribute, ConceptFactory::attribute);
CONCEPT_ITERATOR_HELPER(Entity, ConceptFactory::entity);
CONCEPT_ITERATOR_HELPER(Relation, ConceptFactory::relation);

}  // namespace TypeDB