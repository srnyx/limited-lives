import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.utility.setupAnnoyingAPI
import xyz.srnyx.gradlegalaxy.utility.spigotAPI


plugins {
    java
    id("xyz.srnyx.gradle-galaxy") version "1.3.3"
    id("com.gradleup.shadow") version "8.3.8"
}

spigotAPI("1.8.8")
setupAnnoyingAPI("2fa992b25a", "xyz.srnyx", "4.0.0", "Each player has a limited number of lives. If you die, you are punished")

repository(Repository.PLACEHOLDER_API, Repository.ENGINE_HUB)
dependencies {
    compileOnly("me.clip", "placeholderapi", "2.11.6")
    compileOnly("com.sk89q.worldguard", "worldguard-bukkit", "7.0.0")
}
