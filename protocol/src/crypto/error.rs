use std::error;
use thiserror::Error;

pub type CryptoResult<T> = Result<T, CryptoError>;

#[derive(Error, Debug)]
pub enum CryptoError {
  /* Generic errors */
  #[error("encrypt error: {0}")]
  EncryptError(Box<dyn error::Error + Send + Sync>),
  #[error("decrypt error: {0}")]
  DecryptError(Box<dyn error::Error + Send + Sync>)
}
