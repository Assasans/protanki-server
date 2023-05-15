use std::{
  collections::BTreeMap,
  io::{Write, Read},
  fmt::{Debug, Formatter}
};

use crate::codec::{CodecRegistry, Codec, CodecResult, CodecError, CodecRegistryExt};
use super::{RegisteredPacket, Packet, RegisteredPacketImpl, internal::register_packets};

#[derive(Default)]
pub struct PacketRegistry {
  codec_registry: CodecRegistry,
  packet_codecs: BTreeMap<i32, Box<dyn RegisteredPacket>>,
}

impl PacketRegistry {
  pub fn new() -> Self {
    let mut registry = PacketRegistry::default();

    registry.codec_registry.register_primitives();
    register_packets(&mut registry);

    registry
  }

  pub fn register_packet<T: Packet + Codec<Target = T> + 'static>(&mut self, packet: T) {
    let packet_id = packet.packet_id();
    let codec = self.codec_registry.register_codec(packet);

    if let Some(_) = self.packet_codecs.insert(packet_id, Box::new(RegisteredPacketImpl { codec })) {
      panic!("tried to register packet {} twice", packet_id);
    }
  }

  pub fn encode(&self, writer: &mut dyn Write, packet: &dyn Packet) -> CodecResult<()> {
    let registered_packet = self.packet_codecs.get(&packet.packet_id())
      .ok_or_else(|| CodecError::NoCodec(format!("Packet[{}]@{}", packet.packet_id(), packet.packet_name())))?;

    registered_packet.encode(&self.codec_registry, writer, packet)
  }

  pub fn decode(&self, reader: &mut dyn Read, packet_id: i32) -> CodecResult<Option<Box<dyn Packet>>> {
    Ok(match self.packet_codecs.get(&packet_id) {
      Some(registered_packet) => Some(registered_packet.decode(&self.codec_registry, reader)?),
      None => None
    })
  }
}

impl Debug for PacketRegistry {
  fn fmt(&self, formatter: &mut Formatter<'_>) -> std::fmt::Result {
    formatter.debug_struct("PacketRegistry")
      .field("codec_registry", &"CodecRegistry { .. }")
      .field("packet_codecs", &self.packet_codecs.len())
      .finish()
  }
}
