package io.lucin

import arc.struct.Seq
import arc.util.Log
import arc.util.serialization.Base64Coder
import io.lucin.core.Entity
import mindustry.Vars
import mindustry.core.*
import mindustry.gen.Groups
import mindustry.net.Packets.ConnectPacket
import java.time.Duration
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.security.SecureRandom
import java.util.Base64

var counter = 0

fun main(args: Array<String>) {
    init()

    val config = parseArgs(args)

    if (config == null) {
        printHelp()
        return
    }

    if (config.solo) {
        runSolo(config)
        return
    }

    runStress(config)
}

private data class Config(
    val target: String,
    val rps: Int,
    val duration: Duration,
    val solo: Boolean,
    val uuidOverride: String?,
    val usidOverride: String?
)

private fun parseArgs(args: Array<String>): Config? {
    var target: String? = null
    var rps = 1
    var durationSeconds: Long = 10
    var solo = false
    var uuidOverride: String? = null
    var usidOverride: String? = null

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--target" -> {
                if (i + 1 < args.size) target = args[i + 1]
                i += 1
            }
            "--rps" -> {
                if (i + 1 < args.size) rps = args[i + 1].toIntOrNull() ?: return null
                i += 1
            }
            "--duration" -> {
                if (i + 1 < args.size) durationSeconds = args[i + 1].toLongOrNull() ?: return null
                i += 1
            }
            "--solo" -> solo = true
            "--uuid" -> {
                if (i + 1 < args.size) uuidOverride = args[i + 1]
                i += 1
            }
            "--usid" -> {
                if (i + 1 < args.size) usidOverride = args[i + 1]
                i += 1
            }
        }
        i += 1
    }

    if (target == null) return null
    if (rps <= 0) return null
    if (durationSeconds <= 0) return null

    return Config(target!!, rps, Duration.ofSeconds(durationSeconds), solo, uuidOverride, usidOverride)
}

private fun printHelp() {
    Log.info("Usage: --target <host:port> [--rps <n>] [--duration <sec>] [--solo] [--uuid <id>] [--usid <id>]")
}

private fun init() {
    Groups.init()
    Vars.world = World()

    Vars.content = ContentLoader()
    Vars.content.createBaseContent()

    Vars.state = GameState()

    Version.build = 146
}

private fun packet(config: Config): ConnectPacket {
    val packet = ConnectPacket()

    packet.version = Version.build
    packet.versionType = "official"

    packet.name = "client-" + counter++
    packet.color = 228
    packet.locale = "en"

    packet.mods = Seq()
    packet.mobile = false

    packet.uuid = config.uuidOverride ?: uuid()
    packet.usid = config.usidOverride ?: usid()

    Log.info("uuid=${packet.uuid} usid=${packet.usid}")

    return packet
}

private fun runSolo(config: Config) {
    val parts = config.target.split(':')
    val ip = parts[0]
    val port = parts[1].toInt()

    Log.info("Solo: connecting single client to ${config.target}")
    val entity = Entity.EntityBuilder(false, packet(config), ip, port, port)

    val timer = Timer()
    timer.schedule(object : TimerTask() {
        override fun run() {
            entity.stop()
            Log.info("Solo: client stopped")
        }
    }, 3000)

    Thread.sleep(4000)
}

private fun runStress(config: Config) {
    val parts = config.target.split(':')
    val ip = parts[0]
    val port = parts[1].toInt()

    val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    val periodNanos = 1_000_000_000L / config.rps

    Log.info("Stress: target=${config.target} rps=${config.rps} duration=${config.duration.seconds}s")

    val start = System.nanoTime()
    val task = Runnable {
        val elapsed = System.nanoTime() - start
        if (elapsed >= config.duration.toNanos()) {
            return@Runnable
        }
        try {
            Entity.EntityBuilder(false, packet(config), ip, port, port)
        } catch (_: Exception) {
        }
    }

    scheduler.scheduleAtFixedRate(task, 0, periodNanos, TimeUnit.NANOSECONDS)

    Thread.sleep(config.duration.toMillis())
    scheduler.shutdownNow()
    Log.info("Stress: completed")
}

private val secureRng: SecureRandom = SecureRandom()

private fun uuid(): String {
    val bytes = ByteArray(8)
    fillUnique(bytes)
    return Base64.getEncoder().encodeToString(bytes)
}

private fun usid(): String {
    val bytes = ByteArray(16)
    fillUnique(bytes)
    return Base64.getEncoder().encodeToString(bytes)
}

private fun fillUnique(bytes: ByteArray) {
    do {
        secureRng.nextBytes(bytes)
        val c = counter
        bytes[0] = (bytes[0].toInt() xor (c and 0xFF)).toByte()
        bytes[1] = (bytes[1].toInt() xor ((c ushr 8) and 0xFF)).toByte()
    } while (allZero(bytes))
}

private fun allZero(bytes: ByteArray): Boolean {
    for (b in bytes) if (b.toInt() != 0) return false
    return true
}
