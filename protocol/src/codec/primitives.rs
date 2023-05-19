use std::io::{Write, Read};
use byteorder::{BigEndian, WriteBytesExt, ReadBytesExt};

use super::{Codec, CodecResult};

macro_rules! codec_impl {
  ($target:ty, $encode:ident, $decode:ident) => {
    impl Codec for $target {
      fn encode<W: Write + ?Sized>(&self, writer: &mut W) -> CodecResult<()> {
        writer.$encode(*self)?;
        Ok(())
      }

      fn decode<R: Read + ?Sized>(&mut self, reader: &mut R) -> CodecResult<()> {
        *self = reader.$decode()?;
        Ok(())
      }
    }
  };
  ($target:ty, $encode:ident, $decode:ident, $endianness:ty) => {
    impl Codec for $target {
      fn encode<W: Write + ?Sized>(&self, writer: &mut W) -> CodecResult<()> {
        writer.$encode::<$endianness>(*self)?;
        Ok(())
      }

      fn decode<R: Read + ?Sized>(&mut self, reader: &mut R) -> CodecResult<()> {
        *self = reader.$decode::<$endianness>()?;
        Ok(())
      }
    }
  };
}

codec_impl!(i8, write_i8, read_i8);
codec_impl!(u8, write_u8, read_u8);
codec_impl!(i16, write_i16, read_i16, BigEndian);
codec_impl!(i32, write_i32, read_i32, BigEndian);
codec_impl!(i64, write_i64, read_i64, BigEndian);
codec_impl!(f32, write_f32, read_f32, BigEndian);
codec_impl!(f64, write_f64, read_f64, BigEndian);

impl Codec for bool {
  fn encode<W: Write + ?Sized>(&self, writer: &mut W) -> CodecResult<()> {
    writer.write_u8(if *self { 1 } else { 0 })?;
    Ok(())
  }

  fn decode<R: Read + ?Sized>(&mut self, reader: &mut R) -> CodecResult<()> {
    *self = reader.read_u8()? == 1;
    Ok(())
  }
}
