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

package(default_visibility = ["//visibility:public"])

load("@bazel_skylib//rules:common_settings.bzl", "string_flag")
load("@rules_rust//rust:defs.bzl", "rust_library")
load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("//rust/tests/behaviour:defs.bzl", "crate_features_common")

string_flag(
    name = "mode",
    build_setting_default = "core",
)

config_setting(
    name = "core",
    flag_values = {
        ":mode": "core",
    },
)

config_setting(
    name = "cloud",
    flag_values = {
        ":mode": "cloud",
    },
)

rust_library(
    name = "config",
    srcs = [":lib.rs"],
    crate_features = select({
        ":core": crate_features_common,
        ":cloud": crate_features_common + ["cloud"],
    }),
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    exclude = glob([
        "Cargo.*",
    ]),
    license_type = "apache-header",
    size = "small",
)
