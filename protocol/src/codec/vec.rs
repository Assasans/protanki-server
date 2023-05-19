use std::io::{Write, Read};

use super::{Codec, CodecResult};

impl<T: Codec + Default> Codec for Vec<T> {
  fn encode<W: Write + ?Sized>(&self, writer: &mut W) -> CodecResult<()> {
    (self.len() as i32).encode(writer)?;
    for value in self.iter() {
      value.encode(writer)?;
    }

    Ok(())
  }

  fn decode<R: Read + ?Sized>(&mut self, reader: &mut R) -> CodecResult<()> {
    let mut length = i32::default();
    length.decode(reader)?;

    self.reserve_exact(length as usize);

    for _ in 0..length {
      let mut value = T::default();
      value.decode(reader)?;

      self.push(value);
    }

    Ok(())
  }
}
