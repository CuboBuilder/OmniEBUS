package io.lucin.core

import arc.net.*
import arc.util.Log.*
import arc.util.Threads
import arc.util.io.Reads
import mindustry.Vars.*
import mindustry.gen.*
import mindustry.io.SaveIO
import mindustry.maps.Map
import mindustry.net.*
import mindustry.net.ArcNetProvider.PacketSerializer
import mindustry.net.Packets.*
import java.io.DataInputStream
import java.net.DatagramPacket
import java.util.zip.InflaterInputStream

object Entity {
    class EntityBuilder(
        internal val hidden: Boolean,
        internal val packet: ConnectPacket,
        host: String,
        tcpPort: Int,
        udpPort: Int
    ) {
        internal val client: Client = Client(8192, 8192, PacketSerializer())

        init {
            client.setDiscoveryPacket { DatagramPacket(ByteArray(516), 516) }

            client.addListener(EntityListener(this))
            client.addListener(ChatListener())

            try {
                client.stop()

                Threads.daemon("CLIENT#${packet.uuid}") {
                    try {
                        client.run()
                    } catch (e: Exception) {
                        err(e)
                    }
                }

                client.connect(5000, host, tcpPort, udpPort)
                Net(ArcNetProvider()).handleClient(WorldStream::class.java) { data ->
                    NetworkIO.loadWorld(InflaterInputStream(data.stream))
                }
            } catch (e: Exception) {
                err(e)
            }
        }
    }

    private class EntityListener(val entityBuilder: EntityBuilder) : NetListener {
        override fun connected(connection: Connection?) {
            val connect = Connect()

            if (connection != null) {
                connect.addressTCP = connection.remoteAddressTCP.address.hostAddress

                if (connection.remoteAddressTCP != null) {
                    connect.addressTCP = connection.remoteAddressTCP.toString()

                    info("Connecting to ${connect.addressTCP}")

                    val confirmCallPacket = ConnectConfirmCallPacket()
                    confirmCallPacket.player = Player.create()

                    entityBuilder.client.sendTCP(entityBuilder.packet)

                    if (!entityBuilder.hidden) {
                        entityBuilder.client.sendTCP(confirmCallPacket)
                        info("Confirmed")
                    }
                }
            }
        }

        override fun disconnected(connection: Connection?, reason: DcReason?) {
            if (reason != null) {
                warn("Disconnected. Reason: $reason.")
            }
        }
    }

    private class ChatListener : NetListener {
        override fun received(connection: Connection?, packet: Any?) {
            when (packet) {
                is SendMessageCallPacket -> {
                    try {
                        packet.handled()
                    } catch (e: Exception) {
                        err(e)
                    }

                    info(packet.message)
                }

                is SendMessageCallPacket2 -> {
                    try {
                        packet.handled()
                    } catch (e: Exception) {
                        err(e)
                    }

                    info(packet.message)
                }

                is SendChatMessageCallPacket -> {
                    try {
                        packet.handled()
                    } catch (e: Exception) {
                        err(e)
                    }

                    info(packet.message)
                }
            }
        }
    }

    private class DataListener : NetListener {
        override fun received(connection: Connection?, packet: Any?) {
            when (packet) {
                is StreamBegin -> {
                    packet.handled()
                    info("Start of data reception. Type: ${packet.type}. Id: ${packet.id}. Total: ${packet.total}.")
                }

                is StreamChunk -> {
                    packet.handled()
                    info("Receiving data. Id: ${packet.id}. Size: ${packet.data.size}.")
                    SaveIO.getSaveWriter().readMap(DataInputStream(packet.data.inputStream()), world.context)
                }

                is WorldStream -> {
                    try {
                        packet.handled()
                        info("Start of world data reception.")

                        val inflaterInputStream = InflaterInputStream(packet.stream)
                        val stream = DataInputStream(inflaterInputStream)
                        state.map = Map(SaveIO.getSaveWriter().readStringMap(stream))

                        val read = Reads(stream)

                        Groups.clear()
                        val id: Int = stream.readInt()
                        player.reset()
                        player.read(read)
                        player.id = id
                        player.add()

                        SaveIO.getSaveWriter().readContentHeader(stream)
                        SaveIO.getSaveWriter().readMap(stream, world.context)
                        SaveIO.getSaveWriter().readTeamBlocks(stream)
                        SaveIO.getSaveWriter().readCustomChunks(stream)
                    } catch (e: Exception) {
                        err(e)
                    } finally {
                        content.setTemporaryMapper(null)
                    }
                }
            }
        }
    }
}