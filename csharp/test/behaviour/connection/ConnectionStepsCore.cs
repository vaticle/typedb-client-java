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

using System;
using System.Collections.Generic;
using System.Threading;
using Xunit.Gherkin.Quick;

using com.vaticle.typedb.driver;
using com.vaticle.typedb.driver.Api;
using com.vaticle.typedb.driver.Test.Behaviour.Connection;

namespace com.vaticle.typedb.driver.Test.Behaviour.Connection
{
    public class ConnectionStepsCore: ConnectionStepsBase
    {
        protected override void BeforeAllOnce()
        {
            base.BeforeAllOnce();
//            try
//            {
//                TypeDBCoreRunner typeDBCoreRunner = new TypeDBCoreRunner(serverOptions);
//                TypeDBSingleton.setTypeDBRunner(typeDBCoreRunner);
//                typeDBCoreRunner.start();
//            }
//            catch (InterruptedException | java.util.concurrent.TimeoutException | java.io.IOException e)
//            {
//                e.printStackTrace();
//            }
            Console.WriteLine("CORE Before All!");
        }

        public ConnectionStepsCore()
            : base()
        {Console.WriteLine("Core constr!");}

        public override void Dispose()
        {
            base.Dispose();
            Console.WriteLine("Core Dispose!");
        }

// TODO: Void instead of ITypeDBDriver for now
        public override void CreateTypeDBDriver(string address)
        {
//            return TypeDB.CoreDriver(address);
        }

        [Given(@"typedb starts")]
        [When(@"typedb starts")]
        public override void TypeDBStarts()
        {
            base.TypeDBStarts();
        }

        [Given(@"connection opens with default authentication")]
        [When(@"connection opens with default authentication")]
        public override void ConnectionOpensWithDefaultAuthentication()
        {
//            driver = CreateTypeDBDriver(TypeDBSingleton.GetTypeDBRunner().Address());
            Console.WriteLine("Core: ConnectionOpensWithDefaultAuthentication");
        }

        [Given(@"connection has been opened")]
        public override void ConnectionHasBeenOpened()
        {
            base.ConnectionHasBeenOpened();
        }

        [When(@"connection closes")]
        public override void ConnectionCloses()
        {
            base.ConnectionCloses();
        }

        [Given(@"connection does not have any database")]
        [Then(@"connection does not have any database")]
        public override void ConnectionDoesNotHaveAnyDatabase()
        {
            base.ConnectionDoesNotHaveAnyDatabase();
        }
    }
}