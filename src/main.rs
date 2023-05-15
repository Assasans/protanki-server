pub mod network;

use tokio::net::TcpSocket;
use anyhow::Result;
use futures_util::StreamExt;
use protocol::crypto::{XorCryptoContext, XorCryptoMode};
use protocol::packet::{PacketDowncast, packets};
use tokio::sync::RwLock;
use tracing::{debug, info};
use tracing_subscriber::EnvFilter;

use crate::network::Connection;
use crate::network::server::GameServer;

async fn handle_connection(connection: Connection) -> Result<()> {
  debug!("connection: {:?}", connection);

  let connection = Arc::new(RwLock::new(connection));

  {
    let key = XorCryptoContext::generate_key();
    let mut lock = connection.write().await;
    lock.send(&packets::s2c::session::InitializeEncryption {
      protection_data: key.clone()
    })?;
    lock.crypto_context = Box::new(XorCryptoContext::new(XorCryptoMode::Server, &key));
  }

  let cloned = connection.clone();
  tokio::spawn(async move {
    let mut it = cloned.write().await;
    while let Some(packet) = it.next().await {
      debug!("packet: {:?}", packet);
    }
    debug!("exit");
  });

  Ok(())
}

#[tokio::main]
async fn main() -> Result<()> {
  tracing_subscriber::fmt()
    .with_max_level(tracing::Level::DEBUG)
    .with_env_filter(EnvFilter::from_default_env())
    .init();

  info!("Hello, 世界!");

  let mut server = GameServer::new("127.0.0.1:1337").await?;
  while let Some(connection) = server.next().await {
    handle_connection(connection?).await?;
  }

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
