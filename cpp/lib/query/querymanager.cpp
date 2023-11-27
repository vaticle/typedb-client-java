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

#include "typedb/query/querymanager.hpp"
#include "typedb/common/errormessage.hpp"
#include "typedb/common/exception.hpp"
#include "typedb/common/future.hpp"
#include "typedb/connection/transaction.hpp"

#include "typedb/concept/value/value.hpp"

#include "../inc/macros.hpp"

#define CHECK_QUERY(QUERY)                                                              \
    {                                                                                   \
        if ("" == (QUERY)) throw TypeDBDriverException::of(&QueryError::MISSING_QUERY); \
    }

namespace TypeDB {

QueryManager::QueryManager(TypeDB::Transaction* parentTransaction)
    : parentTransaction(parentTransaction) {}

VoidFuture QueryManager::define(const std::string& query, const Options& options) const {
    CHECK_NATIVE(parentTransaction);
    CHECK_QUERY(query);
    WRAPPED_NATIVE_CALL(VoidFuture, _native::query_define(parentTransaction->getNative(), query.c_str(), options.getNative()));
}

VoidFuture QueryManager::undefine(const std::string& query, const Options& options) const {
    CHECK_NATIVE(parentTransaction);
    CHECK_QUERY(query);
    WRAPPED_NATIVE_CALL(VoidFuture, _native::query_undefine(parentTransaction->getNative(), query.c_str(), options.getNative()));
}

ConceptMapIterable QueryManager::get(const std::string& query, const Options& options) const {
    CHECK_NATIVE(parentTransaction);
    CHECK_QUERY(query);
    WRAPPED_NATIVE_CALL(ConceptMapIterable, _native::query_get(parentTransaction->getNative(), query.c_str(), options.getNative()));
}

#pragma message("You still have to do JSON")
JSONIterable QueryManager::fetch(const std::string& query, const Options& options) const {
    CHECK_NATIVE(parentTransaction);
    CHECK_QUERY(query);
    WRAPPED_NATIVE_CALL(JSONIterable, _native::query_fetch(parentTransaction->getNative(), query.c_str(), options.getNative()));
}

AggregateFuture QueryManager::getAggregate(const std::string& query, const Options& options) const {
    CHECK_NATIVE(parentTransaction);
    CHECK_QUERY(query);
    WRAPPED_NATIVE_CALL(AggregateFuture, _native::query_get_aggregate(parentTransaction->getNative(), query.c_str(), options.getNative()));
}

ConceptMapGroupIterable QueryManager::getGroup(const std::string& query, const Options& options) const {
    CHECK_NATIVE(parentTransaction);
    CHECK_QUERY(query);
    WRAPPED_NATIVE_CALL(ConceptMapGroupIterable, _native::query_get_group(parentTransaction->getNative(), query.c_str(), options.getNative()));
}

ValueGroupIterable QueryManager::getGroupAggregate(const std::string& query, const Options& options) const {
    CHECK_NATIVE(parentTransaction);
    CHECK_QUERY(query);
    WRAPPED_NATIVE_CALL(ValueGroupIterable, _native::query_get_group_aggregate(parentTransaction->getNative(), query.c_str(), options.getNative()));
}


ConceptMapIterable QueryManager::insert(const std::string& query, const Options& options) const {
    CHECK_NATIVE(parentTransaction);
    CHECK_QUERY(query);
    WRAPPED_NATIVE_CALL(ConceptMapIterable, _native::query_insert(parentTransaction->getNative(), query.c_str(), options.getNative()));
}

VoidFuture QueryManager::matchDelete(const std::string& query, const Options& options) const {
    CHECK_NATIVE(parentTransaction);
    CHECK_QUERY(query);
    WRAPPED_NATIVE_CALL(VoidFuture, _native::query_delete(parentTransaction->getNative(), query.c_str(), options.getNative()));
}

ConceptMapIterable QueryManager::update(const std::string& query, const Options& options) const {
    CHECK_NATIVE(parentTransaction);
    CHECK_QUERY(query);
    WRAPPED_NATIVE_CALL(ConceptMapIterable, _native::query_update(parentTransaction->getNative(), query.c_str(), options.getNative()));
}

ExplanationIterable QueryManager::explain(const Explainable& explainable, const Options& options) const {
    CHECK_NATIVE(parentTransaction);
    WRAPPED_NATIVE_CALL(ExplanationIterable, _native::query_explain(parentTransaction->getNative(), explainable.getNative(), options.getNative()));
}

}  // namespace TypeDB