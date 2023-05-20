// Copyright (c) 2023 M. Hadenfeldt
// Copyright (c) 2023 Daniil Pryima
// Licensed under the MIT license

use std::{
  collections::BTreeMap,
  io::{Write, Read},
  fmt::{Debug, Formatter}
};

use crate::codec::{Codec, CodecResult, CodecError};
use crate::packet::{PacketDowncast, RegisteredPacket, internal, RegisteredPacketImpl};
use super::{Packet};

#[derive(Default)]
pub struct PacketRegistry {
  packets: BTreeMap<i32, Box<dyn RegisteredPacket>>
}

impl PacketRegistry {
  pub fn new() -> Self {
    let mut registry = Self::default();
    internal::register_packets(&mut registry);
    registry
  }

  pub fn register_packet<T: Packet + Codec + Default + Sync + 'static>(&mut self, packet: T) {
    let packet_id = packet.packet_id();
    if let Some(_) = self.packets.insert(packet_id, Box::new(RegisteredPacketImpl::<T>::new())) {
      panic!("tried to register packet {} twice", packet_id);
    }
  }

  pub fn encode<W: Write>(&self, writer: &mut W, packet: &dyn Packet) -> CodecResult<()> {
    let registered_packet = self.packets.get(&packet.packet_id())
      .ok_or_else(|| CodecError::NoCodec(format!("Packet[{}]@{}", packet.packet_id(), packet.packet_name())))?;

    registered_packet.encode(writer, packet)
  }

  pub fn decode<R: Read>(&self, reader: &mut R, packet_id: i32) -> CodecResult<Option<Box<dyn Packet>>> {
    Ok(match self.packets.get(&packet_id) {
      Some(registered_packet) => Some(registered_packet.decode(reader)?),
      None => None
    })
  }
}

impl Debug for PacketRegistry {
  fn fmt(&self, formatter: &mut Formatter<'_>) -> std::fmt::Result {
    formatter.debug_struct("PacketRegistry")
      .field("packets", &self.packets.len())
      .finish()
  }
}
