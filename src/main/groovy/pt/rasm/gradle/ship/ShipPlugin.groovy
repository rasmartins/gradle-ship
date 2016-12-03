package pt.rasm.gradle.ship

import de.undercouch.gradle.tasks.download.DownloadExtension
import org.apache.commons.io.FileUtils
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.internal.os.OperatingSystem

class ShipPlugin implements Plugin<Project> {
    static GROUP = "Application Shipping"

    void apply(Project project) {
        project.extensions.create("ship", ShipPluginExtension)

        project.afterEvaluate {
            project.task("bundleAll", group: GROUP, dependsOn: project.tasks.matching { task -> task.name.startsWith("bundleDist") }) {
            }

            project.bundler.jvms.vms.each { jvm ->
                createDownloadTask(project, jvm)
                if (jvm.os == "windows") {
                    createNsisTask(project, jvm)
                } else {
                    createTarTask(project, jvm)
                }
            }
        }
    }

    static void ensureDestinationFolder(Project project) {
        if (!project.bundler.destination)
            project.bundler.destination = project.file("${project.buildDir}/bundles")
    }

    static String getPackageName(Project project, Jvm jvm) {
        return "${project.bundler.product.id}-${project.bundler.product.version}-${jvm.os}-${jvm.arch}"
    }

    static File getPackageDir(Project project) {
        project.file("${project.buildDir}/bundles/dist")
    }

    static void createDownloadTask(Project project, Jvm jvm) {
        project.task("downloadJvm${jvm.safeName()}", group: GROUP) {
            description "Download Oracle Java Runtime Environment packages ($jvm)"

            doLast {
                ensureDestinationFolder(project)

                File jreFolder = project.file("${project.bundler.destination}/jvms")
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

    static CopySpec copyContents(Project project, Jvm jvm) {
        return project.copySpec {
            with {
                into('jvm') {
                    from("${project.bundler.destination}/jvms/${jvm.folderName()}")
                }
            }

            with(project.bundler.contents)
            from(project.bundler.launcherJar)
            from(project.bundler.launcherExe)
            from(project.bundler.launcherJvmArgs) {
                filter(ReplaceTokens, tokens: [JARNAME: "${project.name}-${project.version}.jar".toString()])
                filteringCharset = 'UTF-8'
            }
            from(project.bundler.launcherJvmEnvs)
        }
    }

    static void createTarTask(Project project, Jvm jvm) {
        project.task("bundleDist${jvm.safeName()}", type: Tar, group: GROUP, dependsOn: "downloadJvm${jvm.safeName()}") {
            ensureDestinationFolder(project)

            archiveName getPackageName(project, jvm) + ".tar.gz"
            destinationDir getPackageDir(project)
            compression Compression.GZIP

            into(getPackageName(project, jvm)) {
                with(copyContents(project, jvm))
            }
        }
    }

    static void createNsisTask(Project project, Jvm jvm) {
        project.task("bundleDist${jvm.safeName()}", group: GROUP, dependsOn: "downloadJvm${jvm.safeName()}") {
            doLast {
                ensureDestinationFolder(project)

                def script = "${jvm.toString()}.nsi"
                def scriptFolder = project.file("${project.bundler.destination}/nsis")
                def scriptPath = project.file("$scriptFolder/$script").absolutePath
                def distFolder = project.file("${project.bundler.destination}/nsis/dist-${jvm.os}-${jvm.arch}")
                def outFile = project.file("${getPackageDir(project)}/${getPackageName(project, jvm)}.exe")

                def coreTokens = [
                        PRODUCT_NAME   : project.bundler.product.name,
                        PRODUCT_ID     : project.bundler.product.id,
                        PRODUCT_VERSION: project.bundler.product.version,
                        LICENSE        : project.bundler.product.license.absolutePath,
                        ICON           : project.bundler.product.installIcon.absolutePath,
                        HEADER         : project.bundler.product.installHeader.absolutePath,
                        EXE            : project.bundler.product.id + ".exe",
                        DISTRIBUTION   : distFolder.absolutePath,
                        ARCH           : jvm.arch,
                        OUTFILE        : outFile.absolutePath

                ]

                // Generate NSIS script.
                project.copy {
                    from project.bundler.nsisTemplate
                    into scriptFolder
                    filter(ReplaceTokens, tokens: coreTokens)
                    filter(ReplaceTokens, tokens: project.bundler.nsisTokens)
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