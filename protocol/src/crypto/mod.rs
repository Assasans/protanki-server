mod error;
pub use error::*;

mod empty;
pub use empty::*;

mod xor;
pub use xor::*;

pub trait CryptoContext: Send {
  fn encrypt(&mut self, buffer: &mut [u8]) -> CryptoResult<()>;
  fn decrypt(&mut self, buffer: &mut [u8]) -> CryptoResult<()>;
}
