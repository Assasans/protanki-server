use std::io::{Write, Read};

use super::{Codec, CodecResult, CodecError, CodecRegistry, CodecRegistryExt};

#[derive(Default)]
pub struct StringCodec;

impl Codec for StringCodec {
  type Target = String;

  fn encode(&self, registry: &CodecRegistry, writer: &mut dyn Write, value: &Self::Target) -> CodecResult<()> {
    if value.is_empty() {
      registry.encode(writer, &true)?;
      return Ok(());
    }

    let bytes = value.as_bytes();
    registry.encode(writer, &false)?;
    registry.encode(writer, &(bytes.len() as i32))?;
    writer.write_all(bytes)?;

    Ok(())
  }

  fn decode(&self, registry: &CodecRegistry, reader: &mut dyn Read) -> CodecResult<Self::Target> {
    if registry.decode::<bool>(reader)? {
      return Ok(String::new());
    }

    let length = registry.decode::<i32>(reader)? as usize;
    let mut buffer = Vec::with_capacity(length);
    buffer.resize(length, 0);
    reader.read_exact(&mut buffer)?;

    Ok(String::from_utf8(buffer).map_err(|error| CodecError::DecodeError(Box::new(error)))?)
  }
}
