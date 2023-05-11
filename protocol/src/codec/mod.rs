mod registry;
pub use registry::*;

mod error;
pub use error::*;

mod primitives;
mod string;
mod vec;

pub mod codecs {
  pub use super::primitives::*;
  pub use super::string::*;
  pub use super::vec::*;
}

#[cfg(test)]
mod test;

use std::{
  any::Any,
  io::{Read, Write},
  sync::Arc,
  ops::Deref
};

pub type CodecRef<T> = Arc<dyn Codec<Target = T>>;

// Type alias does not downcasts from Any for some reason
pub(self) struct CodecRefInternal<T>(Arc<dyn Codec<Target = T>>);

impl<T> Deref for CodecRefInternal<T> {
  type Target = Arc<dyn Codec<Target = T>>;

  fn deref(&self) -> &Self::Target {
    &self.0
  }
}

pub trait Codec: Send + Sync {
  type Target: Sized + Any;

  fn encode(&self, registry: &CodecRegistry, writer: &mut dyn Write, value: &Self::Target) -> CodecResult<()>;
  fn decode(&self, registry: &CodecRegistry, reader: &mut dyn Read) -> CodecResult<Self::Target>;
}
