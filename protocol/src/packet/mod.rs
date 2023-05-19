mod registry;
pub use registry::*;

mod registered;
pub use registered::*;

mod unknown;
pub use unknown::*;

pub mod enums;
pub mod structs;
include!(concat!(env!("OUT_DIR"), "/packets.rs"));

use std::{any::Any, fmt::Debug};

pub trait Packet: Debug + Send {
  fn as_any(&self) -> &dyn Any;
  fn as_any_mut(&mut self) -> &mut dyn Any;

  fn packet_name(&self) -> &str;
  fn packet_id(&self) -> i32;
  fn model_id(&self) -> i32;
}

pub trait PacketDowncast {
  fn downcast_ref<T: 'static>(&self) -> Option<&T>;
}

impl PacketDowncast for dyn Packet + '_ {
  fn downcast_ref<T: 'static>(&self) -> Option<&T> {
    self.as_any().downcast_ref::<T>()
  }
}
