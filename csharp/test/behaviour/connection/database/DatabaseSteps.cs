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

using DataTable = Gherkin.Ast.DataTable;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Xunit;

using com.vaticle.typedb.driver;
using com.vaticle.typedb.driver.Test.Behaviour.Connection;

namespace com.vaticle.typedb.driver.Test.Behaviour.Connection.Database
{
    public class DatabaseSteps
    {
        public void ConnectionCreateDatabase(string name)
        {
            ConnectionFixture.Driver.Databases().Create(name);
        }

        public void ConnectionCreateDatabases(DataTable names)
        {
            foreach (var row in names.Rows)
            {
                foreach (var name in row.Cells)
                {
                    ConnectionCreateDatabase(name.Value);
                }
            }
        }

        public void ConnectionCreateDatabasesInParallel(DataTable names)
        {
            var collectedNames = new List<string>();
            foreach (var row in names.Rows)
            {
                foreach (var name in row.Cells)
                {
                    collectedNames.Add(name.Value);
                }
            }

            int workerThreads;
            int ioThreads;
            ThreadPool.GetAvailableThreads(out workerThreads, out ioThreads);
            Assert.True(workerThreads > collectedNames.Count);

            Task[] taskArray = new Task[collectedNames.Count];
            for (int i = 0; i < taskArray.Length; i++)
            {
                var name = collectedNames[i];
                taskArray[i] = Task.Factory.StartNew(() =>
                    {
                        ConnectionCreateDatabase(name);
                    });
            }

            Task.WaitAll(taskArray);
        }

        public void ConnectionDeleteDatabase(string name)
        {
            ConnectionFixture.Driver.Databases().Get(name).Delete();
        }

        public void ConnectionDeleteDatabases(DataTable names)
        {
            foreach (var row in names.Rows)
            {
                foreach (var name in row.Cells)
                {
                    ConnectionDeleteDatabase(name.Value);
                }
            }
        }

        public void ConnectionDeleteDatabaseThrowsException(string databaseName)
        {
            Assert.Throws<Common.Exception.TypeDBDriverException>(
                () => ConnectionFixture.Driver.Databases().Get(databaseName).Delete());
        }

        public void ConnectionDeleteDatabasesInParallel(DataTable names)
        {
            var collectedNames = new List<string>();
            foreach (var row in names.Rows)
            {
                foreach (var name in row.Cells)
                {
                    collectedNames.Add(name.Value);
                }
            }

            int workerThreads;
            int ioThreads;
            ThreadPool.GetAvailableThreads(out workerThreads, out ioThreads);
            Assert.True(workerThreads > collectedNames.Count);

            Task[] taskArray = new Task[collectedNames.Count];
            for (int i = 0; i < collectedNames.Count; ++i)
            {
                var name = collectedNames[i];
                taskArray[i] = Task.Factory.StartNew(() =>
                    {
                        ConnectionDeleteDatabase(name);
                    });
            }

            Task.WaitAll(taskArray);
        }

        public void ConnectionHasDatabase(string name)
        {
            Assert.True(ConnectionFixture.Driver.Databases().Contains(name));
        }

        public void ConnectionHasDatabases(DataTable names)
        {
            int expectedDatabasesSize = 0;

            foreach (var row in names.Rows)
            {
                foreach (var name in row.Cells)
                {
                    ConnectionHasDatabase(name.Value);
                    expectedDatabasesSize++;
                }
            }

            // TODO: Could there be just == ? The description is more like >=!
            Assert.True(expectedDatabasesSize >= ConnectionFixture.Driver.Databases().GetAll().Count);
        }

        public void ConnectionDoesNotHaveDatabase(string name)
        {
            Assert.False(ConnectionFixture.Driver.Databases().Contains(name));
        }

        public void ConnectionDoesNotHaveDatabases(DataTable names)
        {
            foreach (var row in names.Rows)
            {
                foreach (var name in row.Cells)
                {
                    ConnectionDoesNotHaveDatabase(name.Value);
                }
            }
        }

        public void ConnectionDoesNotHaveAnyDatabase()
        {
            Assert.NotNull(ConnectionFixture.Driver);
            Assert.True(ConnectionFixture.Driver.IsOpen());
            Assert.Equal(0, ConnectionFixture.Driver.Databases().GetAll().Count);
        }
    }
}
