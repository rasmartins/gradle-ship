package pt.rasm.gradle.ship

import org.apache.commons.lang3.StringUtils

class Jvm {
    // Base URL.
    def url
    // Operating system name.
    String os
    // Computer architecture.
    String arch
    // Major version number.
    int major
    // Update number.
    int update
    // Build number.
    int build

    def url() {
        def version = "${major}u${update}"
        return new URL("${url}/${version}-b${build}/jre-${version}-${os}-${arch}.tar.gz")
    }

    def folderName() {
        return "jre-${major}u${update}-b${build}-${os}-${arch}"
    }

    def safeName() {
        return StringUtils.capitalize(os) + StringUtils.capitalize(arch)
    }

    String toString() {
        return "${os}-${arch}-${major}u${update}-b${build}"
    }
}
