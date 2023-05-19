use std::io::{Write, Read};

use super::{Codec, CodecResult};

impl<T: Codec + Default> Codec for Option<T> {
  fn encode<W: Write + ?Sized>(&self, writer: &mut W) -> CodecResult<()> {
    self.is_none().encode(writer)?;
    if let Some(value) = self.as_ref() {
      value.encode(writer)?;
    }

    Ok(())
  }

  fn decode<R: Read + ?Sized>(&mut self, reader: &mut R) -> CodecResult<()> {
    let mut is_null = bool::default();
    is_null.decode(reader)?;

    *self = if is_null {
      None
    } else {
      let mut value = T::default();
      value.decode(reader)?;

      Some(value)
    };

    Ok(())
  }
}
