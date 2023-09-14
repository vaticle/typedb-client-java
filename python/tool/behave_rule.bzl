#
# Copyright (C) 2022 Vaticle
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

# =============================================================================
# Description: Adds a test rule for the BDD tool behave to the bazel rule set.
# Knowledge:
# * https://bazel.build/versions/master/docs/skylark/cookbook.html
# * https://bazel.build/versions/master/docs/skylark/rules.html
# * https://bazel.build/versions/master/docs/skylark/lib/ctx.html
# * http://pythonhosted.org/behave/gherkin.html
# =============================================================================

load("@vaticle_typedb_driver_pip//:requirements.bzl", "requirement")


def py_behave_test(*, name, background = None, native_typedb_artifact, steps, feats, deps, data=[], typedb_port, **kwargs):
    feats_dir = "features-" + name
    steps_out_dir = feats_dir + "/steps"

    native.genrule(
        name = name + "_features",
        cmd = "mkdir $(@D)/" + feats_dir
                + " && cp $(location %s) $(@D)/%s" % (feats[0], feats_dir)
                + " && cp $(location %s) $(@D)/%s" % (background[0], feats_dir)
                + " && mkdir $(@D)/" + steps_out_dir + " && "
                + " && ".join(["cp $(location %s) $(@D)/%s" % (step_file, steps_out_dir) for step_file in steps]),
        srcs = steps + background + feats,
        outs = [feats_dir],
    ) # create directory structure as above

    native.py_test( # run behave with the above as data
        name = name,
        data = data + [name + "_features"],
        deps = deps + [requirement("behave"), requirement("PyHamcrest")],
        srcs = ["//python/tests/behaviour:entry_point_behave.py"],
        args = ["$(location :" + name + "_features" + ")", "--no-capture", "-D", "port=" + typedb_port],
        main = "//python/tests/behaviour:entry_point_behave.py",
    )


def typedb_behaviour_py_test(*, name, background_core = None, background_cluster = None, **kwargs):
    if background_core:
        py_behave_test(
            name = name + "-core",
            background = background_core,
            native_typedb_artifact = "@//tool/test:native-typedb-artifact",
            toolchains = ["@rules_python//python:current_py_toolchain"],
            typedb_port = "1729",
            **kwargs,
        )

    if background_cluster:
        py_behave_test(
            name = name + "-cluster",
            background = background_cluster,
            native_typedb_artifact = "@//tool/test:native-typedb-cluster-artifact",
            toolchains = ["@rules_python//python:current_py_toolchain"],
            typedb_port = "11729",
            **kwargs,
        )
