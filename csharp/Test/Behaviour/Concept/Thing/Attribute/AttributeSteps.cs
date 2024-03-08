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

using DataTable = Gherkin.Ast.DataTable; // TODO Remove if not needed
using DocString = Gherkin.Ast.DocString; // TODO Remove if not needed
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Xunit;
using Xunit.Gherkin.Quick;

using Vaticle.Typedb.Driver;
using Vaticle.Typedb.Driver.Api;
using Vaticle.Typedb.Driver.Common;

namespace Vaticle.Typedb.Driver.Test.Behaviour
{
    public partial class BehaviourSteps
    {
        [When(@"attribute\\( ?{type_label} ?) get instances contain: {var}")]
        public void AttributeTypeGetInstancesContain(string typeLabel, string var)
        {
            var instances = SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .GetInstances(SingleTransaction);

            Assert.True(instances.anyMatch(i => i.equals(Get(var))));
        }

        [Then(@"attribute {var} get owners contain: {var}")]
        public void AttributeGetOwnersContain(string var1, string var2)
        {
            Assert.True(Get(var1).asAttribute().GetOwners(SingleTransaction).anyMatch(o => o.equals(Get(var2))));
        }

        [Then(@"attribute {var} get owners do not contain: {var}")]
        public void AttributeGetOwnersDoNotContain(string var1, string var2)
        {
            Assert.True(Get(var1).asAttribute().GetOwners(SingleTransaction).noneMatch(o => o.equals(Get(var2))));
        }

        [Then(@"attribute {var} has value type: {valueType}")]
        public void AttributeHasValueType(string var, Value.Type valueType)
        {
            Assert.Equals(valueType, Get(var).asAttribute().GetType().GetValueType());
        }

        [When(@"{var} = attribute\\( ?{type_label} ?) as\\( ?boolean ?) put: {bool}")]
        public void AttributeTypeAsBooleanPut(string var, string typeLabel, bool value)
        {
            var attributeType = SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Put(SingleTransaction, value).Resolve();

            Put(var, attributeType);
        }

        [Then(@"attribute\\( ?{type_label} ?) as\\( ?boolean ?) put: {bool}; throws exception")]
        public void AttributeTypeAsBooleanPutThrowsException(string typeLabel, bool value)
        {
            Assert.Throws<TypeDBDriverException>(() => SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Put(SingleTransaction, value).Resolve());
        }

        [When(@"{var} = attribute\\( ?{type_label} ?) as\\( ?long ?) put: {int}")]
        public void AttributeTypeAsLongPut(string var, string typeLabel, long value)
        {
            var attributeType = SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Put(SingleTransaction, value).Resolve();

            Put(var, attributeType);
        }

        [Then(@"attribute\\( ?{type_label} ?) as\\( ?long ?) put: {int}; throws exception")]
        public void AttributeTypeAsLongPutThrowsException(string typeLabel, long value)
        {
            Assert.Throws<TypeDBDriverException>(() => SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Put(SingleTransaction, value).Resolve());
        }

        [When(@"{var} = attribute\\( ?{type_label} ?) as\\( ?double ?) put: {double}")]
        public void AttributeTypeAsDoublePut(string var, string typeLabel, double value)
        {
            var attributeType = SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Put(SingleTransaction, value).Resolve();

            Put(var, attributeType);
        }

        [Then(@"attribute\\( ?{type_label} ?) as\\( ?double ?) put: {double}; throws exception")]
        public void AttributeTypeAsDoublePutThrowsException(string typeLabel, double value)
        {
            Assert.Throws<TypeDBDriverException>(() => SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Put(SingleTransaction, value).Resolve());
        }

        [When(@"{var} = attribute\\( ?{type_label} ?) as\\( ?string ?) put: {word}")]
        public void AttributeTypeAsStringPut(string var, string typeLabel, string value)
        {
            var attributeType = SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Put(SingleTransaction, value).Resolve();

            Put(var, attributeType);
        }

        [Then(@"attribute\\( ?{type_label} ?) as\\( ?string ?) put: {word}; throws exception")]
        public void AttributeTypeAsStringPutThrowsException(string typeLabel, string value)
        {
            Assert.Throws<TypeDBDriverException>(() => SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Put(SingleTransaction, value).Resolve());
        }

        [When(@"{var} = attribute\\( ?{type_label} ?) as\\( ?datetime ?) put: {datetime}")]
        public void AttributeTypeAsDatetimePut(string var, string typeLabel, DateTime value)
        {
            var attributeType = SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Put(SingleTransaction, value).Resolve();

            Put(var, attributeType);
        }

        [Then(@"attribute\\( ?{type_label} ?) as\\( ?datetime ?) put: {datetime}; throws exception")]
        public void AttributeTypeAsDatetimePutThrowsException(string typeLabel, DateTime value)
        {
            var exception = Assert.Throws<TypeDBDriverException>(() => SingleTransaction
                .Concepts.GetAttributeType(typeLabel).Resolve()
                .Put(SingleTransaction, value));

            Console.WriteLine(exception.Message); // TODO: Just for debug, remove later.
        }

        [When(@"{var} = attribute\\( ?{type_label} ?) as\\( ?boolean ?) get: {bool}")]
        public void AttributeTypeAsBooleanGet(string var, string typeLabel, bool value)
        {
            var attributeType = SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Get(SingleTransaction, value).Resolve();

            Put(var, attributeType);
        }

        [When(@"{var} = attribute\\( ?{type_label} ?) as\\( ?long ?) get: {int}")]
        public void AttributeTypeAsLongGet(string var, string typeLabel, long value)
        {
            var attributeType = SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Get(SingleTransaction, value).Resolve();

            Put(var, attributeType);
        }

        [When(@"{var} = attribute\\( ?{type_label} ?) as\\( ?double ?) get: {double}")]
        public void AttributeTypeAsDoubleGet(string var, string typeLabel, double value)
        {
            var attributeType = SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Get(SingleTransaction, value).Resolve();

            Put(var, attributeType);
        }

        [When(@"{var} = attribute\\( ?{type_label} ?) as\\( ?string ?) get: {word}")]
        public void AttributeTypeAsStringGet(string var, string typeLabel, string value)
        {
            var attributeType = SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Get(SingleTransaction, value).Resolve();

            Put(var, attributeType);
        }

        [When(@"{var} = attribute\\( ?{type_label} ?) as\\( ?datetime ?) get: {datetime}")]
        public void AttributeTypeAsDatetimeGet(string var, string typeLabel, DateTime value)
        {
            var attributeType = SingleTransaction
                .Concepts
                .GetAttributeType(typeLabel).Resolve()
                .Get(SingleTransaction, value).Resolve();

            Put(var, attributeType);
        }

        [Then(@"attribute {var} has bool value: {bool}")]
        public void AttributeHasBooleanValue(string var, bool value)
        {
            Assert.Equals(value, Get(var).asAttribute().GetValue().asBoolean());
        }

        [Then(@"attribute {var} has long value: {long}")]
        public void AttributeHasLongValue(string var, long value)
        {
            Assert.Equals(value, Get(var).asAttribute().GetValue().asLong());
        }

        [Then(@"attribute {var} has double value: {double}")]
        public void AttributeHasDoubleValue(string var, double value)
        {
            Assert.Equals(value, Get(var).asAttribute().GetValue().asDouble(), 0.0001);
        }

        [Then(@"attribute {var} has string value: {word}")]
        public void AttributeHasStringValue(string var, string value)
        {
            Assert.Equals(value, Get(var).asAttribute().GetValue().asString());
        }

        [Then(@"attribute {var} has datetime value: {datetime}")]
        public void AttributeHasDatetimeValue(string var, DateTime value)
        {
            Assert.Equals(value, Get(var).asAttribute().GetValue().asDateTime());
        }
    }
}