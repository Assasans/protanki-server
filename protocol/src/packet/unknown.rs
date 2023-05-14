use std::{fmt::Debug, any::{Any, type_name}};

use super::Packet;

pub struct UnknownPacket {
  packet_id: i32,
  payload: Vec<u8>
}

impl UnknownPacket {
  pub fn new(packet_id: i32, payload: Vec<u8>) -> Self {
    Self { packet_id, payload }
  }

  pub fn payload(&self) -> &[u8] {
    &self.payload
  }
}

impl Debug for UnknownPacket {
  fn fmt(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
    formatter.debug_struct("UnknownPacket")
      .field("packet_id", &self.packet_id)
      .field("payload_length", &self.payload.len())
      .finish()
  }
}

impl Packet for UnknownPacket {
  fn as_any(&self) -> &dyn Any {
    self
  }

  fn as_any_mut(&mut self) -> &mut dyn Any {
    self
  }

  fn packet_name(&self) -> &str {
    type_name::<Self>()
  }

  fn packet_id(&self) -> i32 {
    self.packet_id
  }

  fn model_id(&self) -> i32 {
    i32::MIN
  }
}
