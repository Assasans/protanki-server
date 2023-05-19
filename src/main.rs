pub mod network;

use std::env;
use std::io::stdout;
use std::sync::Arc;
use tokio::net::TcpSocket;
use anyhow::Result;
use futures_util::StreamExt;
use protocol::crypto::{XorCryptoContext, XorCryptoMode};
use protocol::packet::{PacketDowncast, packets};
use tokio::sync::RwLock;
use tracing::{debug, info};
use tracing_subscriber::{EnvFilter, Layer};
use tracing_subscriber::layer::SubscriberExt;
use tracing_subscriber::util::SubscriberInitExt;

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
  let console = tracing_subscriber::fmt::layer()
    .with_writer(Arc::new(stdout()))
    .and_then(EnvFilter::from_default_env());

  let file = tracing_subscriber::fmt::layer()
    .with_writer(tracing_appender::rolling::hourly("logs", "protanki-server"))
    .with_ansi(false)
    .and_then(EnvFilter::from_default_env());

  tracing_subscriber::registry()
    .with(console)
    .with(file)
    .init();

  info!("Hello, 世界!");

  // let mut server = GameServer::new("127.0.0.1:1337").await?;
  // while let Some(connection) = server.next().await {
  //   handle_connection(connection?).await?;
  // }

  let socket = TcpSocket::new_v4()?;
  let stream = socket.connect("146.59.110.195:1337".parse().unwrap()).await?;
  let mut connection = Connection::new(Box::new(stream));

  while let Some(packet) = connection.next().await {
    let packet = packet?;
    debug!("packet: {:?}", packet);

    if let Some(packets::s2c::session::InitializeEncryption { protection_data }) = packet.downcast_ref() {
      debug!("key: {:?}", protection_data);

      connection.crypto_context = Box::new(XorCryptoContext::new(XorCryptoMode::Client, &protection_data));
      connection.send(&packets::c2s::session::EncryptionInitialized {
        lang: Some("ru".to_owned())
      })?;
    } else if let Some(packets::s2c::session::resources::LoadDependencies { callback_id, .. }) = packet.downcast_ref() {
      connection.send(&packets::c2s::session::resources::DependenciesLoaded {
        callback_id: *callback_id
      })?;
    } else if let Some(packets::s2c::session::resources::ResourcesLoaded {}) = packet.downcast_ref() {
      let username = env::var("PT_USERNAME").unwrap();
      let password = env::var("PT_PASSWORD").unwrap();

      connection.send(&packets::c2s::auth::UsernameLogin {
        login: Some(username),
        password: Some(password),
        remember: false
      })?;
    }
  }

  Ok(())
}
