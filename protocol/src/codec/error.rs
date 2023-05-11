use std::{io, error};
use thiserror::Error;

pub type CodecResult<T> = Result<T, CodecError>;

impl From<io::Error> for CodecError {
  fn from(error: io::Error) -> Self {
    Self::IoError(error)
  }
}

#[derive(Error, Debug)]
pub enum CodecError {
  #[error("no codec registered for `{0}`")]
  NoCodec(String),

  #[error("an IO error occurred `{0}`")]
  IoError(io::Error),
  #[error("invalid argument: {0}")]
  InvalidArgument(String),

  /* Generic errors */
  #[error("encode error: {0}")]
  EncodeError(Box<dyn error::Error>),
  #[error("decode error: {0}")]
  DecodeError(Box<dyn error::Error>)
}
