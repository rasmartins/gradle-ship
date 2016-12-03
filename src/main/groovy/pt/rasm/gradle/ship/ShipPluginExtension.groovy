//******************************************************************************
// Copyright (C) 2016 Ricardo Martins                                          *
//******************************************************************************
// Licensed under the Apache License, Version 2.0 (the "License");             *
// you may not use this file except in compliance with the License. You may    *
// obtain a copy of the License at                                             *
//                                                                             *
// http://www.apache.org/licenses/LICENSE-2.0                                  *
//                                                                             *
// Unless required by applicable law or agreed to in writing, software         *
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT   *
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.            *
// See the License for the specific language governing permissions and         *
// limitations under the License.                                              *
//******************************************************************************

package pt.rasm.gradle.ship

class ShipPluginExtension {
    /** Product details. */
    def product
    /** Runtime environments. */
    def jvms
    /** Destination folder for generated files. */
    def destination
    /** NSIS template. */
    def nsisTemplate = getClass().getClassLoader().getResource("nsis/template.nsi")
    /** NSIS template tokens. */
    def nsisTokens = []
    /** Launcher JAR. */
    def launcherJar = getClass().getClassLoader().getResource("launcher/launcher.jar")
    def launcherJvmArgs = getClass().getClassLoader().getResource("launcher/jvm.args")
    def launcherJvmEnvs = getClass().getClassLoader().getResource("launcher/jvm.envs")
    // Bundle contents (CopySpec compatible).
    def contents

    def javaRuntime(args, closure) {
        jvms = new Jvms(args)
        jvms.with(closure)
    }

    def product(args) {
        product = new Product()
        product.with(args)
    }
}
