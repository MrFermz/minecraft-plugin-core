// minecraft-plugin-core — shared API/library, loaded as its OWN plugin on the
// server. Other plugins compile against it (`compileOnly`) and talk to it at
// runtime through Bukkit's ServicesManager. See CLAUDE.md for the rationale.

dependencies {
    compileOnly(libs.paper.api)
    // Loaded at runtime by Paper's library loader (see plugin.yml `libraries`),
    // not bundled — compile-only here. Only HikariCP is referenced in code; the
    // JDBC drivers are loaded by class name at runtime per the configured engine.
    compileOnly(libs.hikari)
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "hikariVersion" to libs.versions.hikari.get(),
        "sqliteVersion" to libs.versions.sqlite.get(),
        "postgresqlVersion" to libs.versions.postgresql.get(),
        "mariadbVersion" to libs.versions.mariadb.get(),
    )
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}
