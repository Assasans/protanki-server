pub mod network;

use tokio::net::TcpSocket;
use anyhow::Result;
use futures_util::StreamExt;
use protocol::crypto::{XorCryptoContext, XorCryptoMode};
use protocol::packet::{PacketDowncast, packets};
use tracing::{debug, info};
use tracing_subscriber::EnvFilter;

use crate::network::Connection;

#[tokio::main]
async fn main() -> Result<()> {
  tracing_subscriber::fmt()
    .with_max_level(tracing::Level::DEBUG)
    .with_env_filter(EnvFilter::from_default_env())
    .init();

  info!("Hello, 世界!");

  let socket = TcpSocket::new_v4()?;
  let stream = socket.connect("146.59.110.195:1337".parse().unwrap()).await?;
  let mut connection = Connection::new(Box::new(stream));

  while let Some(packet) = connection.next().await {
    debug!("packet: {:?}", packet);

    if let Some(packets::s2c::session::InitializeEncryption { protection_data }) = packet?.downcast_ref() {
      debug!("key: {:?}", protection_data);

      connection.crypto_context = Box::new(XorCryptoContext::new(XorCryptoMode::Client, &protection_data));
      connection.send(&packets::c2s::session::EncryptionInitialized {
        lang: "ru".to_owned()
      })?;
    }
  }

  Ok(())
}
