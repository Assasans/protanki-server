// Copyright (c) 2023 M. Hadenfeldt
// Copyright (c) 2023 Daniil Pryima
// Licensed under the MIT license

use std::{
  io::{Write, Read},
  sync::Arc,
  any::type_name,
  marker::PhantomData
};

use crate::codec::{Codec, CodecResult, CodecError};
use super::Packet;

pub trait RegisteredPacket: Send + Sync {
  fn packet_name(&self) -> &str;

  fn encode(&self, writer: &mut dyn Write, packet: &dyn Packet) -> CodecResult<()>;
  fn decode(&self, reader: &mut dyn Read) -> CodecResult<Box<dyn Packet>>;
}

pub struct RegisteredPacketImpl<T> {
  _marker: PhantomData<T>
}

impl<T> RegisteredPacketImpl<T> {
  pub fn new() -> Self {
    Self {
      _marker: PhantomData
    }
  }
}

impl<T: Packet + Codec + Default + Sync + 'static> RegisteredPacket for RegisteredPacketImpl<T> {
  fn packet_name(&self) -> &str {
    type_name::<T>()
  }

  fn encode(&self, writer: &mut dyn Write, packet: &dyn Packet) -> CodecResult<()> {
    let packet = match packet.as_any().downcast_ref::<T>() {
      Some(packet) => packet,
      None => return Err(CodecError::DowncastError(packet.packet_name().to_owned()))
    };

    packet.encode(writer)
  }

  fn decode(&self, reader: &mut dyn Read) -> CodecResult<Box<dyn Packet>> {
    let mut packet = T::default();
    packet.decode(reader)?;

    Ok(Box::new(packet))
  }
}
