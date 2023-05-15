use std::{io::{Write, Read}, sync::Arc, any::type_name};

use crate::codec::{CodecRegistry, CodecResult, Codec, CodecError};
use super::Packet;

pub trait RegisteredPacket: Send + Sync {
  fn packet_name(&self) -> &str;

  fn encode(&self, registry: &CodecRegistry, writer: &mut dyn Write, packet: &dyn Packet) -> CodecResult<()>;
  fn decode(&self, registry: &CodecRegistry, reader: &mut dyn Read) -> CodecResult<Box<dyn Packet>>;
}

pub struct RegisteredPacketImpl<T: Packet + Codec<Target = T> + Send + 'static> {
  pub codec: Arc<dyn Codec<Target = T>>
}

impl<T: Packet + Codec<Target = T> + Send + 'static> RegisteredPacket for RegisteredPacketImpl<T> {
  fn packet_name(&self) -> &str {
    type_name::<T>()
  }

  fn encode(&self, registry: &CodecRegistry, writer: &mut dyn Write, packet: &dyn Packet) -> CodecResult<()> {
    let packet = match packet.as_any().downcast_ref::<T>() {
      Some(packet) => packet,
      None => return Err(CodecError::DowncastError(packet.packet_name().to_owned()))
    };

    self.codec.encode(registry, writer, packet)
  }

  fn decode(&self, registry: &CodecRegistry, reader: &mut dyn Read) -> CodecResult<Box<dyn Packet>> {
    let packet = self.codec.decode(registry, reader)?;
    Ok(Box::new(packet))
  }
}
