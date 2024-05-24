import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.utility.setupAnnoyingAPI
import xyz.srnyx.gradlegalaxy.utility.spigotAPI


plugins {
    java
    id("xyz.srnyx.gradle-galaxy") version "1.1.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

setupAnnoyingAPI("ef15b8c14f", "xyz.srnyx", "3.0.1", "Each player has a limited number of lives. If you die, you are punished")
spigotAPI("1.8.8")
repository(Repository.PLACEHOLDER_API)
repository("https://maven.enginehub.org/repo/")

dependencies {
    compileOnly("me.clip", "placeholderapi", "2.11.3")
    compileOnly("com.sk89q.worldguard", "worldguard-bukkit", "7.0.0")
}
