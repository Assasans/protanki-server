package jp.assasans.protanki.server

import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

internal class PacketProcessorTest {
  private lateinit var packetProcessor: PacketProcessor

  @BeforeTest
  fun setUp() {
    packetProcessor = PacketProcessor()
  }

  @Test
  fun get0Packets() {
    packetProcessor.tryGetPacket().let { packet ->
      assertNull(packet)
    }
  }

  @Test
  fun get1Packet() {
    val content = "system;get_aes_data;RU"

    packetProcessor.write("${content}end~".toByteArray())
    packetProcessor.tryGetPacket().let { packet ->
      assertNotNull(packet)
      assertEquals(content, packet)
    }
    packetProcessor.tryGetPacket().let { packet ->
      assertNull(packet)
    }
  }

  @Test
  fun getManyPackets() {
    fun getPacket(index: Int): String = "test;packet;${index}"

    val times = 10
    val content = buildString {
      repeat(times) { index ->
        append(getPacket(index))
        append("end~")
      }
    }

    packetProcessor.write(content.toByteArray())
    repeat(times) { index ->
      packetProcessor.tryGetPacket().let { packet ->
        assertNotNull(packet)
        assertEquals(getPacket(index), packet)
      }
    }

    packetProcessor.tryGetPacket().let { packet ->
      assertNull(packet)
    }
  }

  @Test
  fun getMultipleWrites() {
    val part1 = "system;"
    val part2 = "get_aes_data;"
    val part3 = "RU"

    packetProcessor.write(part1.toByteArray())
    assertNull(packetProcessor.tryGetPacket())

    packetProcessor.write(part2.toByteArray())
    assertNull(packetProcessor.tryGetPacket())

    packetProcessor.write(part3.toByteArray())
    assertNull(packetProcessor.tryGetPacket())

    packetProcessor.write("end~".toByteArray())
    packetProcessor.tryGetPacket().let { packet ->
      assertNotNull(packet)
      assertEquals("$part1$part2$part3", packet)
    }

    assertNull(packetProcessor.tryGetPacket())
  }
}
