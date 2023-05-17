use std::io::{Write, Read};
use byteorder::{BigEndian, WriteBytesExt, ReadBytesExt};

use super::{Codec, CodecRegistry, CodecResult};

macro_rules! impl_singlebyte_codec {
  ($target:ty, $name:ident, $encode:ident, $decode:ident) => {
    #[derive(Default)]
    pub struct $name;

    impl Codec for $name {
      type Target = $target;

      fn encode(&self, _registry: &CodecRegistry, writer: &mut dyn Write, target: &Self::Target) -> CodecResult<()> {
        Ok(writer.$encode(*target)?)
      }

      fn decode(&self, _registry: &CodecRegistry, reader: &mut dyn Read) -> CodecResult<Self::Target> {
        Ok(reader.$decode()?)
      }
    }
  };
}

macro_rules! impl_multibyte_codec {
  ($target:ty, $name:ident, $encode:ident, $decode:ident) => {
    #[derive(Default)]
    pub struct $name;

    impl Codec for $name {
      type Target = $target;

      fn encode(&self, _registry: &CodecRegistry, writer: &mut dyn Write, target: &Self::Target) -> CodecResult<()> {
        Ok(writer.$encode::<BigEndian>(*target)?)
      }

      fn decode(&self, _registry: &CodecRegistry, reader: &mut dyn Read) -> CodecResult<Self::Target> {
        Ok(reader.$decode::<BigEndian>()?)
      }
    }
  };
}

impl_singlebyte_codec!(i8, ByteCodec, write_i8, read_i8);
impl_singlebyte_codec!(u8, UByteCodec, write_u8, read_u8);
impl_multibyte_codec!(i16, ShortCodec, write_i16, read_i16);
impl_multibyte_codec!(i32, IntCodec, write_i32, read_i32);
impl_multibyte_codec!(i64, LongCodec, write_i64, read_i64);
impl_multibyte_codec!(f32, FloatCodec, write_f32, read_f32);
impl_multibyte_codec!(f64, DoubleCodec, write_f64, read_f64);

#[derive(Default)]
pub struct BoolCodec;

impl Codec for BoolCodec {
  type Target = bool;

  fn encode(&self, _registry: &CodecRegistry, writer: &mut dyn Write, value: &Self::Target) -> CodecResult<()> {
    Ok(writer.write_u8(if *value { 1 } else { 0 })?)
  }

  fn decode(&self, _registry: &CodecRegistry, reader: &mut dyn Read) -> CodecResult<Self::Target> {
    Ok(reader.read_u8().map(|value| value == 1)?)
  }
}

#[cfg(test)]
mod test {
  use crate::codec::{
    CodecRegistry,
    codecs::*,
    test::{impl_test_decode_eq_encode, assert_decode_eq_encode}
  };

  impl_test_decode_eq_encode!(i8, ByteCodec::default());
  impl_test_decode_eq_encode!(i16, ShortCodec::default());
  impl_test_decode_eq_encode!(i32, IntCodec::default());
  impl_test_decode_eq_encode!(i64, LongCodec::default());
  impl_test_decode_eq_encode!(f32, FloatCodec::default());
  impl_test_decode_eq_encode!(f64, DoubleCodec::default());

  #[test]
  fn bool_decode_eq_encode() {
    let codec = BoolCodec::default();
    let registry = CodecRegistry::new();

    assert_decode_eq_encode!(codec, registry, false);
    assert_decode_eq_encode!(codec, registry, true);
  }

  #[test]
  fn string_decode_eq_encode() {
    let codec = StringCodec::default();
    let mut registry = CodecRegistry::new();
    registry.register_codec(BoolCodec::default());
    registry.register_codec(IntCodec::default());

    assert_decode_eq_encode!(codec, registry, "Hello world!".to_owned());
    assert_decode_eq_encode!(codec, registry, "{}".to_owned());
  }
}
