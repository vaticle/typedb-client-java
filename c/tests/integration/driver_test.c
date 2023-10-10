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

#include <stdio.h>

#include "c/typedb_driver.h"
#include "common.h"

// Connection, database management, concept api, queries

int test_basic_query() {
    Connection* conn = 0;
    DatabaseManager* dbMgr = 0;
    Session* session = 0;
    Transaction* transaction = 0;
    Options* opts = 0;

    int ret = 1;

    conn = connection_open_core(TYPEDB_CORE_ADDRESS);
    if (0 == conn) goto cleanup;

    dbMgr = database_manager_new(conn);
    if (0 == dbMgr || check_error()) goto cleanup;

    delete_database_if_exists(dbMgr, "test");
    if (check_error()) goto cleanup;

    databases_create(dbMgr, "test");
    if (check_error()) goto cleanup;

    opts = options_new();
    if (0 == opts) goto cleanup;

    session = session_new(dbMgr, "test", Schema, opts);
    if (0 == session) goto cleanup;

    transaction = transaction_new(session, Write, opts);
    if (0 == transaction) goto cleanup;

    query_define(transaction, "define person sub entity;", opts);
    if (check_error()) goto cleanup;

    ConceptMapIterator* it = query_match(transaction, "match $t sub thing;", opts);
    ConceptMap* conceptMap;
    printf("Results:\n");
    while (0 != (conceptMap = concept_map_iterator_next(it))) {
        Concept* concept = concept_map_get(conceptMap, "t");
        char* label = thing_type_get_label(concept);
        printf("- %s\n", label);
        free(label);
        concept_drop(concept);
        concept_map_drop(conceptMap);
    }
    concept_map_iterator_drop(it);

    transaction_commit(transaction);
    transaction = 0;
    ret = 0;

cleanup:
    if (check_error()) print_error(__FILE__, __LINE__);
    transaction_drop(transaction);
    session_drop(session);
    options_drop(opts);

    delete_database_if_exists(dbMgr, "test");
    if (check_error()) print_error(__FILE__, __LINE__);
    database_manager_drop(dbMgr);
    connection_close(conn);
    return ret;
}
