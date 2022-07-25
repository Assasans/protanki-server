package jp.assasans.protanki.server

import java.io.PipedInputStream
import java.io.PipedOutputStream
import mu.KotlinLogging
import jp.assasans.protanki.server.commands.Command

class PacketProcessor {
  private val logger = KotlinLogging.logger {}

  private val output = PipedOutputStream()
  private val input = PipedInputStream(output)

  fun write(data: ByteArray) {
    output.write(data)

    // logger.trace { "Written: ${String(data)}" }
  }

  fun tryGetPacket(): String? {
    var endIndex = 0

    val packetOutput = PipedOutputStream()
    val packetInput = PipedInputStream(packetOutput)

    var read: Int
    while(true) {
      if(input.available() < 1) return null

      read = input.read()
      if(read == -1) break

      val value: Byte = read.toByte()

      packetOutput.write(read)

      if(value == Command.Delimiter[endIndex]) endIndex++
      else endIndex = 0

      if(endIndex == Command.Delimiter.size) {
        packetOutput.close()
        val packet = String(packetInput.readAllBytes().dropLast(Command.Delimiter.size).toByteArray())

        // logger.trace { "End of packet: $packet" }

        return packet
      }
    }
    return null
  }
}
