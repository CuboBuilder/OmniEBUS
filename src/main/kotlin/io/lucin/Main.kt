package io.lucin

import arc.math.Rand
import arc.struct.Seq
import arc.util.serialization.Base64Coder
import io.lucin.core.Entity
import mindustry.Vars
import mindustry.core.*
import mindustry.gen.Groups
import mindustry.gen.SendChatMessageCallPacket
import mindustry.net.Packets.ConnectPacket
import java.lang.Thread.sleep

fun main() {
    init()
    for (i in 0..2) {
        val player = Entity.EntityBuilder(false, packet(), "darkdustry.tk", 10000, 10000)
        sleep(500)
        val packet = SendChatMessageCallPacket()
        packet.message = "Hello! I very love easyplay.su!!!"
        player.client.sendTCP(packet)
        sleep(500)
    }
    while (true) {
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
    packet.locale = "chlen darka"

    packet.mods = Seq()
    packet.mobile = false

    packet.uuid = uuid()
    packet.usid = usid()

    return packet
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
