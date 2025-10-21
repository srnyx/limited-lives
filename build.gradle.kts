import xyz.srnyx.gradlegalaxy.data.config.DependencyConfig
import xyz.srnyx.gradlegalaxy.data.config.JavaSetupConfig
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.utility.setupAnnoyingAPI
import xyz.srnyx.gradlegalaxy.utility.spigotAPI


plugins {
    java
    id("xyz.srnyx.gradle-galaxy") version "2.0.2"
    id("com.gradleup.shadow") version "8.3.9"
}

spigotAPI(config = DependencyConfig("1.8.8"))
setupAnnoyingAPI(
    javaSetupConfig = JavaSetupConfig("xyz.srnyx", "4.2.0", "Each player has a limited number of lives. If you die, you are punished"),
    annoyingAPIConfig = DependencyConfig("3375b21876"))

repository(Repository.PLACEHOLDER_API, Repository.ENGINE_HUB)
dependencies {
    compileOnly("me.clip", "placeholderapi", "2.11.6")
    compileOnly("com.sk89q.worldguard", "worldguard-bukkit", "7.0.0")
}
