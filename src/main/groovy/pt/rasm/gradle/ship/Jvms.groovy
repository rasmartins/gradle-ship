package pt.rasm.gradle.ship

class Jvms {
    // Base JRE URL.
    def baseUrl = 'http://download.oracle.com/otn-pub/java/jdk'
    int majorVersion
    int updateNumber
    int buildNumber
    def vms = []

    def each(Closure closure) {
        vms.each closure
    }

    def jvm(args) {
        def jvm = new Jvm(args)
        jvm.with {
            if (!major)
                major = majorVersion
            if (!update)
                update = updateNumber
            if (!build)
                build = buildNumber
            if (!url)
                url = baseUrl
        }
        vms.add(jvm)
    }
}
