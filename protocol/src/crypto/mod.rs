mod error;
pub use error::*;

mod empty;
pub use empty::*;

mod xor;
pub use xor::*;

use std::fmt::Debug;

pub trait CryptoContext: Debug + Send + Sync {
  fn encrypt(&mut self, buffer: &mut [u8]) -> CryptoResult<()>;
  fn decrypt(&mut self, buffer: &mut [u8]) -> CryptoResult<()>;
}
