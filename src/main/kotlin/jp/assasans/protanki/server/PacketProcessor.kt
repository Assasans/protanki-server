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
    val delimiter = Command.Delimiter.toByteArray()
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

      // TODO(Assasans): Rewrite
      if(endIndex == 0 && value == delimiter[0]) endIndex++
      else if(endIndex == 1 && value == delimiter[1]) endIndex++
      else if(endIndex == 2 && value == delimiter[2]) endIndex++
      else if(endIndex == 3 && value == delimiter[3]) endIndex++
      else endIndex = 0

      if(endIndex == 4) {
        packetOutput.close()
        val packet = String(packetInput.readAllBytes()).dropLast(Command.Delimiter.length)

        // logger.trace { "End of packet: $packet" }

        return packet
      }
    }
    return null
  }
}
