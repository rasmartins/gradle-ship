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

import de.undercouch.gradle.tasks.download.DownloadExtension
import org.apache.commons.io.FileUtils
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.internal.os.OperatingSystem

class ShipPlugin implements Plugin<Project> {
    static GROUP_SHIP = "Application shipping"
    static GROUP_DLOAD = "Download"

    void apply(Project project) {
        project.extensions.create("ship", ShipPluginExtension)

        project.afterEvaluate {
            def shipTasks = []

            project.ship.jvms.vms.each { jvm ->
                createDownloadTask(project, jvm)
                def task;
                if (jvm.os == "windows") {
                    task = createNsisTask(project, jvm)
                } else {
                    task = createTarTask(project, jvm)
                }
                shipTasks.add(task)
            }

            project.task("shipAll", group: GROUP_SHIP, dependsOn: shipTasks) {
                description("Create packages for all configured targets")
            }
        }
    }

    def ensureDestinationFolder(Project project) {
        if (!project.ship.destination)
            project.ship.destination = project.file("${project.buildDir}/ship")
    }

    def getPackageName(Project project, Jvm jvm) {
        return "${project.ship.product.id}-${project.ship.product.version}-${jvm.os}-${jvm.arch}"
    }

    def getPackageDir(Project project) {
        project.file("${project.buildDir}/distributions")
    }

    def createDownloadTask(Project project, Jvm jvm) {
        project.task("getJvm${jvm.safeName()}", group: GROUP_DLOAD) {
            description "Download JRE for platform $jvm.os-$jvm.arch"

            doLast {
                ensureDestinationFolder(project)

                File jreFolder = project.file("${project.ship.destination}/jvms")
                jreFolder.mkdirs()

                def url = jvm.url()
                def tarFile = project.file("${jreFolder}/${jvm.folderName()}.tar.gz")
                def tarFolder = project.file("${jreFolder}/${jvm.folderName()}")

                // Download.
                if (!tarFile.exists()) {
                    def tempFile = project.file("${tarFile}.part")
                    new DownloadExtension(project).configure {
                        src url
                        dest tempFile
                        header 'Cookie', 'oraclelicense=accept-securebackup-cookie'
                        overwrite true
                    }
                    tempFile.renameTo(tarFile)
                }

                // Unpack.
                if (!tarFolder.exists()) {
                    def tempFolder = project.file("${tarFolder}.part")

                    project.copy {
                        from project.tarTree(tarFile)
                        into tempFolder
                        exclude '**/man/**', '**/plugin/**', '**/bin/rmid',
                                '**/bin/rmiregistry', '**/bin/tnameserv', '**/bin/keytool',
                                '**/bin/kinit', '**/bin/klist', '**/bin/ktab',
                                '**/bin/policytool', '**/bin/orbd', '**/bin/servertool',
                                '**/bin/javaws', '**/lib/javaws*'
                    }

                    tempFolder.listFiles()[0].renameTo(tarFolder)
                    FileUtils.deleteDirectory(tempFolder)
                }
            }
        }
    }

    def extractResource(Project project, URL url) {
        String baseName = new File(url.getPath()).getName()
        File destinationFile = project.file("${project.buildDir}/ship/tmp/${baseName}")
        FileUtils.copyInputStreamToFile(url.openStream(), destinationFile)
        return destinationFile
    }

    def getLauncherExe(Jvm jvm) {
        def exe = "launcher/launcher-${jvm.os}-${jvm.arch}.exe"
        return getClass().getClassLoader().getResource(exe)
    }

    def copyContents(Project project, Jvm jvm) {
        return project.copySpec {
            with {
                into('jvm') {
                    from("${project.ship.destination}/jvms/${jvm.folderName()}")
                }
            }

            with(project.ship.contents)
            from(extractResource(project, project.ship.launcherJar))
            from(extractResource(project, getLauncherExe(jvm)))
            from(extractResource(project, project.ship.launcherJvmEnvs))
            from(extractResource(project, project.ship.launcherJvmArgs)) {
                filter(ReplaceTokens, tokens: [JARNAME: "${project.name}-${project.version}.jar".toString()])
                filteringCharset = 'UTF-8'
            }
        }
    }

    def createTarTask(Project project, Jvm jvm) {
        project.task("ship${jvm.safeName()}", type: Tar, group: GROUP_SHIP, dependsOn: "getJvm${jvm.safeName()}") {
            description "Create package for platform $jvm.os-$jvm.arch"

            ensureDestinationFolder(project)

            archiveName getPackageName(project, jvm) + ".tar.gz"
            destinationDir getPackageDir(project)
            compression Compression.GZIP

            into(getPackageName(project, jvm)) {
                with(copyContents(project, jvm))
            }
        }
    }

    def createNsisTask(Project project, Jvm jvm) {
        project.task("ship${jvm.safeName()}", group: GROUP_SHIP, dependsOn: "getJvm${jvm.safeName()}") {
            description "Create installer for platform $jvm.os-$jvm.arch"

            doLast {
                ensureDestinationFolder(project)

                def script = "${jvm.toString()}.nsi"
                def scriptFolder = project.file("${project.ship.destination}/nsis")
                def scriptPath = project.file("$scriptFolder/$script").absolutePath
                def distFolder = project.file("${project.ship.destination}/nsis/dist-${jvm.os}-${jvm.arch}")
                def outFile = project.file("${getPackageDir(project)}/${getPackageName(project, jvm)}.exe")

                def coreTokens = [
                        PRODUCT_NAME   : project.ship.product.name,
                        PRODUCT_ID     : project.ship.product.id,
                        PRODUCT_VERSION: project.ship.product.version,
                        LICENSE        : project.ship.product.license.absolutePath,
                        ICON           : project.ship.product.installIcon.absolutePath,
                        HEADER         : project.ship.product.installHeader.absolutePath,
                        EXE            : project.ship.product.id + ".exe",
                        DISTRIBUTION   : distFolder.absolutePath,
                        ARCH           : jvm.arch,
                        OUTFILE        : outFile.absolutePath

                ]

                // Generate NSIS script.
                project.copy {
                    from extractResource(project, project.ship.nsisTemplate)
                    into scriptFolder
                    filter(ReplaceTokens, tokens: coreTokens)
                    filter(ReplaceTokens, tokens: project.ship.nsisTokens)
                    rename { script }
                    filteringCharset = 'UTF-8'
                }

                // Create distribution.
                FileUtils.deleteDirectory(distFolder)
                copyContents(project, jvm)
                project.copy {
                    into distFolder
                    with(copyContents(project, jvm))
                }

                // Generate installer.
                def makensis = "makensis"
                if (OperatingSystem.current().isWindows())
                    makensis += ".exe"

                project.exec {
                    commandLine makensis, scriptPath
                }

                FileUtils.deleteDirectory(distFolder)
            }
        }
    }
}
