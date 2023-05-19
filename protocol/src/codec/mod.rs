mod error;
pub use error::*;

mod primitives;
mod string;
mod vec;
mod option;

pub mod codecs {
  pub use super::primitives::*;
  pub use super::string::*;
  pub use super::vec::*;
  pub use super::option::*;
}

use std::io::{Read, Write};

pub trait Codec {
  fn encode<W: Write + ?Sized>(&self, writer: &mut W) -> CodecResult<()>;
  fn decode<R: Read + ?Sized>(&mut self, reader: &mut R) -> CodecResult<()>;
}
