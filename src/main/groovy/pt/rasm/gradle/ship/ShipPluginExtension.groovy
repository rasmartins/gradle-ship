package pt.rasm.gradle.ship

class ShipPluginExtension {
    // Product details.
    def product
    // Runtime environments.
    def jvms
    // Destination folder for generated files.
    def destination
    // NSIS template.
    def nsisTemplate = getClass().getClassLoader().getResource("nsis/template.nsi")
    // Launcher JAR.
    def launcherJar = getClass().getClassLoader().getResource("launcher/launcher.jar")
    // Launcher EXE.
    def launcherExe = getClass().getClassLoader().getResource("launcher/launcher.exe")
    def launcherJvmArgs = getClass().getClassLoader().getResource("launcher/jvm.args")
    def launcherJvmEnvs = getClass().getClassLoader().getResource("launcher/jvm.envs")

    // NSIS template tokens
    def nsisTokens = []
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
