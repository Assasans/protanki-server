use super::{CryptoContext, CryptoResult};

#[derive(Default, Debug, Clone)]
pub struct EmptyCryptoContext;

impl EmptyCryptoContext {
  pub fn new() -> Self {
    Default::default()
  }
}

impl CryptoContext for EmptyCryptoContext {
  fn encrypt(&mut self, _buffer: &mut [u8]) -> CryptoResult<()> {
    Ok(())
  }

  fn decrypt(&mut self, _buffer: &mut [u8]) -> CryptoResult<()> {
    Ok(())
  }
}
