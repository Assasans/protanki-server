use rand::Rng;

use super::{CryptoContext, CryptoResult};

#[derive(Debug, PartialEq, Eq, Clone, Copy)]
pub enum XorCryptoMode {
  Server,
  Client
}

#[derive(Debug, Clone)]
pub struct XorCryptoContext {
  decrypt_state: [u8; 8],
  decrypt_state_index: u8,

  encrypt_state: [u8; 8],
  encrypt_state_index: u8
}

impl XorCryptoContext {
  pub fn generate_key() -> Vec<i8> {
    let mut key = vec![0; 4];
    rand::thread_rng().fill(&mut key[..]);

    key
  }

  pub fn new(mode: XorCryptoMode, key: &[i8]) -> Self {
    let seed = key.iter().fold(0, |seed, value| seed ^ value) as u8;

    let mut decrypt_state = [0; 8];
    let mut encrypt_state = [0; 8];

    for index in 0..8 {
      if mode == XorCryptoMode::Server {
        encrypt_state[index as usize] = seed ^ index << 3;
        decrypt_state[index as usize] = seed ^ index << 3 ^ 0x57;
      } else {
        decrypt_state[index as usize] = seed ^ index << 3;
        encrypt_state[index as usize] = seed ^ index << 3 ^ 0x57;
      }
    }

    Self {
      encrypt_state,
      encrypt_state_index: 0,

      decrypt_state,
      decrypt_state_index: 0
    }
  }
}

impl CryptoContext for XorCryptoContext {
  fn encrypt(&mut self, buffer: &mut [u8]) -> CryptoResult<()> {
    for index in 0..buffer.len() {
      let value = buffer[index];

      buffer[index] = value ^ self.encrypt_state[self.encrypt_state_index as usize];
      self.encrypt_state[self.encrypt_state_index as usize] = value;
      self.encrypt_state_index ^= value & 0x7;
    }

    Ok(())
  }

  fn decrypt(&mut self, buffer: &mut [u8]) -> CryptoResult<()> {
    for index in 0..buffer.len() {
      let value = buffer[index];

      self.decrypt_state[self.decrypt_state_index as usize] = value ^ self.decrypt_state[self.decrypt_state_index as usize];
      buffer[index] = self.decrypt_state[self.decrypt_state_index as usize];
      self.decrypt_state_index ^= self.decrypt_state[self.decrypt_state_index as usize] & 0x7;
    }

    Ok(())
  }
}
