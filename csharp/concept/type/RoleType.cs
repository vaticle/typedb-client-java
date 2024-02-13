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
 
using System.Collections.Generic;
using System.Linq;

using Vaticle.Typedb.Driver;
using Vaticle.Typedb.Driver.Api;
using Vaticle.Typedb.Driver.Api.Concept.Type;
using Vaticle.Typedb.Driver.Common;
using Vaticle.Typedb.Driver.Common.Exception;
using Vaticle.Typedb.Driver.Concept.Thing;

namespace Vaticle.Typedb.Driver.Concept.Type
{
    public class RoleType : Type, IRoleType
    {
        public RoleType(Pinvoke.Concept nativeConcept)
            : base(nativeConcept)
        {
        }

        public sealed bool IsRoot()
        {
            return Pinvoke.typedb_driver.role_type_is_root(NativeObject);
        }

        public sealed bool IsAbstract() 
        {
            return Pinvoke.typedb_driver.role_type_is_abstract(NativeObject);
        }

        public Label GetLabel() 
        {
            return new Label(
                Pinvoke.typedb_driver.role_type_get_scope(NativeObject), 
                Pinvoke.typedb_driver.role_type_get_name(NativeObject));
        }

        public sealed VoidPromise Delete(ITypeDBTransaction transaction) 
        {
            return new VoidPromise(Pinvoke.typedb_driver.role_type_delete(
                NativeTransaction(transaction), NativeObject).Resolve);
        }

        public sealed Promise<bool> IsDeleted(ITypeDBTransaction transaction) 
        {
            return new Promise<bool>(Pinvoke.typedb_driver.role_type_is_deleted(
                NativeTransaction(transaction), NativeObject).Resolve);
        }

        public sealed VoidPromise SetLabel(ITypeDBTransaction transaction, string label) 
        {
            return new VoidPromise(Pinvoke.typedb_driver.role_type_set_label(
                NativeTransaction(transaction), NativeObject, label).Resolve);
        }

        public Promise<IRoleType> GetSupertype(ITypeDBTransaction transaction) 
        {
            return Promise.Map<IRoleType, Pinvoke.Concept>(
                Pinvoke.typedb_driver.role_type_get_supertype(
                    NativeTransaction(transaction), NativeObject), 
                obj => new RoleType(obj));
        }

        public sealed ICollection<IRoleType> GetSupertypes(ITypeDBTransaction transaction) 
        {
            try 
            {
                return new NativeEnumerable<Pinvoke.Concept>(
                    Pinvoke.typedb_driver.role_type_get_supertypes(
                        NativeTransaction(transaction), NativeObject))
                    .Select(obj => new RoleType(obj));
            } 
            catch (Pinvoke.Error e) 
            {
                throw new TypeDBDriverException(e);
            }
        }

        public sealed ICollection<IRoleType> GetSubtypes(ITypeDBTransaction transaction)
        {
            return GetSubtypes(transaction, IConcept.Transitivity.TRANSITIVE);
        }

        public sealed ICollection<IRoleType> GetSubtypes(
            ITypeDBTransaction transaction, IConcept.Transitivity transitivity) 
        {
            try 
            {
                return new NativeEnumerable<Pinvoke.Concept>(
                    Pinvoke.typedb_driver.role_type_get_subtypes(
                        NativeTransaction(transaction), NativeObject, transitivity.NativeObject))
                    .Select(obj => new RoleType(obj));
            } 
            catch (Pinvoke.Error e) 
            {
                throw new TypeDBDriverException(e);
            }
        }

        public sealed Promise<IRelationType> GetRelationType(ITypeDBTransaction transaction) 
        {
            return Promise.Map<IRelationType, Pinvoke.Concept>(
                Pinvoke.typedb_driver.role_type_get_relation_type(
                    NativeTransaction(transaction), NativeObject),
                obj => new RelationType(obj));
        }

        public sealed ICollection<IRelationType> GetRelationTypes(ITypeDBTransaction transaction) 
        {
            try 
            {
                return new NativeEnumerable<Pinvoke.Concept>(
                    Pinvoke.typedb_driver.role_type_get_relation_types(
                        NativeTransaction(transaction), NativeObject))
                    .Select(obj => new RelationType(obj));
            } 
            catch (Pinvoke.Error e) 
            {
                throw new TypeDBDriverException(e);
            }
        }

        public sealed ICollection<IThingType> GetPlayerTypes(ITypeDBTransaction transaction)
        {
            return GetPlayerTypes(transaction, IConcept.Transitivity.TRANSITIVE);
        }

        public sealed ICollection<IThingType> GetPlayerTypes(
            ITypeDBTransaction transaction, IConcept.Transitivity transitivity)
        {
            try
            {
                return new NativeEnumerable<Pinvoke.Concept>(
                    Pinvoke.typedb_driver.role_type_get_player_types(
                        NativeTransaction(transaction), NativeObject, transitivity.NativeObject))
                    .Select(obj => new ThingType(obj));
            }
            catch (Pinvoke.Error e)
            {
                throw new TypeDBDriverException(e);
            }
        }

        public sealed ICollection<IRelation> GetRelationInstances(ITypeDBTransaction transaction)
        {
            return GetRelationInstances(transaction, IConcept.Transitivity.TRANSITIVE);
        }

        public sealed ICollection<IRelation> GetRelationInstances(
            ITypeDBTransaction transaction, IConcept.Transitivity transitivity)
        {
            try
            {
                return new NativeEnumerable<Pinvoke.Concept>(
                    Pinvoke.typedb_driver.role_type_get_relation_instances(
                        NativeTransaction(transaction), NativeObject, transitivity.NativeObject))
                    .Select(obj => new Relation(obj));
            }
            catch (Pinvoke.Error e)
            {
                throw new TypeDBDriverException(e);
            }
        }

        public sealed ICollection<IThing> GetPlayerInstances(ITypeDBTransaction transaction)
        {
            return GetPlayerInstances(transaction, IConcept.Transitivity.TRANSITIVE);
        }

        public sealed ICollection<IThing> GetPlayerInstances(
            ITypeDBTransaction transaction, IConcept.Transitivity transitivity)
        {
            try
            {
                return new NativeEnumerable<Pinvoke.Concept>(
                    Pinvoke.typedb_driver.role_type_get_player_instances(
                        NativeTransaction(transaction), NativeObject, transitivity.NativeObject))
                    .Select(obj => new Thing(obj));
            }
            catch (Pinvoke.Error e)
            {
                throw new TypeDBDriverException(e);
            }
        }
    }
}
