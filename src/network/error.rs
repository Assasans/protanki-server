use std::error;
use thiserror::Error;

pub type ConnectionResult<T> = Result<T, ConnectionError>;

#[derive(Error, Debug)]
pub enum ConnectionError {
  #[error("socket EOF")]
  SocketEndOfFile,

  #[error("invalid packet size: {0} bytes")]
  InvalidPacketSize(usize),

  #[error("decode error: {0}")]
  DecodeError(Box<dyn error::Error + Send + Sync>),

  #[error("receive error: {0}")]
  RecvError(Box<dyn error::Error + Send + Sync>),
  #[error("send error: {0}")]
  SendError(Box<dyn error::Error + Send + Sync>)
}
