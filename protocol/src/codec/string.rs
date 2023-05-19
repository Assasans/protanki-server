use std::{
  io::{Write, Read},
  str
};

use super::{Codec, CodecResult, CodecError};

impl Codec for String {
  fn encode<W: Write + ?Sized>(&self, writer: &mut W) -> CodecResult<()> {
    let bytes = self.as_bytes();

    (bytes.len() as i32).encode(writer)?;
    writer.write_all(bytes)?;

    Ok(())
  }

  fn decode<R: Read + ?Sized>(&mut self, reader: &mut R) -> CodecResult<()> {
    let mut length = i32::default();
    length.decode(reader)?;

    // TODO(Assasans/perf): It is possible to get rid of Vec allocation by using String#as_bytes_mut
    let mut buffer = vec![0; length as usize];
    reader.read_exact(&mut buffer)?;

    self.reserve_exact(length as usize);
    self.push_str(str::from_utf8(&buffer).map_err(|error| CodecError::DecodeError(Box::new(error)))?);

    Ok(())
  }
}
