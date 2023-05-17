use std::{
  any::{TypeId, Any, type_name},
  io::{Write, Read},
  sync::Arc,
  collections::BTreeMap
};

use super::{Codec, CodecRef, CodecRefInternal, CodecResult, CodecError};

#[derive(Default)]
pub struct CodecRegistry {
  // Have to use [Any] because there are no Java-like type perameter wildcards
  codecs: BTreeMap<TypeId, Box<dyn Any + Send + Sync>>
}

impl CodecRegistry {
  pub fn new() -> Self {
    Default::default()
  }

  pub fn register_codec<T: 'static>(&mut self, codec: impl Codec<Target = T> + 'static + Sized) -> CodecRef<T> {
    let codec = Arc::new(codec);
    self.codecs.insert(TypeId::of::<T>(), Box::new(CodecRefInternal(codec.clone())));
    codec
  }

  #[must_use]
  pub fn get_codec<T: 'static>(&self) -> Option<CodecRef<T>> {
    self.codecs.get(&TypeId::of::<T>())
      // TODO(Assasans): If downcast fails, function will return None
      .and_then(|codec| codec.downcast_ref::<CodecRefInternal<T>>())
      .map(|codec| (*codec).clone())
  }
}

pub trait CodecRegistryExt {
  fn register_primitives(&mut self);

  #[must_use] fn try_get_codec<T: 'static>(&self) -> CodecResult<CodecRef<T>>;

  #[must_use] fn encode<T: 'static>(&self, writer: &mut dyn Write, value: &T) -> CodecResult<()>;
  #[must_use] fn decode<T: 'static>(&self, reader: &mut dyn Read) -> CodecResult<T>;
}

impl CodecRegistryExt for CodecRegistry {
  fn register_primitives(&mut self) {
    use super::codecs::*;

    self.register_codec(ByteCodec::default());
    self.register_codec(UByteCodec::default());
    self.register_codec(ShortCodec::default());
    self.register_codec(IntCodec::default());
    self.register_codec(LongCodec::default());
    self.register_codec(FloatCodec::default());
    self.register_codec(DoubleCodec::default());

    self.register_codec(BoolCodec::default());
    self.register_codec(StringCodec::default());

    self.register_codec(VectorCodec::<i8>::default());
    self.register_codec(VectorCodec::<u8>::default());
    self.register_codec(VectorCodec::<i16>::default());
    self.register_codec(VectorCodec::<i32>::default());
    self.register_codec(VectorCodec::<i64>::default());
    self.register_codec(VectorCodec::<f32>::default());
    self.register_codec(VectorCodec::<f64>::default());
    self.register_codec(VectorCodec::<bool>::default());
    self.register_codec(VectorCodec::<String>::default());
  }

  #[must_use]
  fn try_get_codec<T: 'static>(&self) -> CodecResult<CodecRef<T>> {
    self.get_codec::<T>().ok_or_else(|| CodecError::NoCodec(type_name::<T>().to_owned()))
  }

  #[must_use]
  fn decode<T: 'static>(&self, reader: &mut dyn Read) -> CodecResult<T> {
    self.try_get_codec::<T>()?.decode(self, reader)
  }

  #[must_use]
  fn encode<T: 'static>(&self, writer: &mut dyn Write, value: &T) -> CodecResult<()> {
    self.try_get_codec::<T>()?.encode(self, writer, value)
  }
}
