use std::{io::{Write, Read}, marker::PhantomData};

use super::{Codec, CodecResult, CodecRegistry, CodecRegistryExt};

#[derive(Default)]
pub struct VectorCodec<T> {
  _marker: PhantomData<T>
}

impl<T: Send + Sync + 'static> Codec for VectorCodec<T> {
  type Target = Vec<T>;

  fn encode(&self, registry: &CodecRegistry, writer: &mut dyn Write, target: &Self::Target) -> CodecResult<()> {
    let codec = registry.try_get_codec::<T>()?;

    registry.encode(writer, &(target.len() as i32))?;
    for entry in target.iter() {
      codec.encode(registry, writer, entry)?;
    }

    Ok(())
  }

  fn decode(&self, registry: &CodecRegistry, reader: &mut dyn Read) -> CodecResult<Self::Target> {
    let codec = registry.try_get_codec::<T>()?;

    let length = registry.decode::<i32>(reader)? as usize;
    let mut result = Vec::with_capacity(length);
    for _ in 0..length {
      result.push(codec.decode(registry, reader)?);
    }

    Ok(result)
  }
}
