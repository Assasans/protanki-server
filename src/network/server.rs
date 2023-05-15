use std::pin::Pin;
use std::task::{Context, Poll};
use anyhow::{anyhow, Result};
use futures_util::Stream;
use tokio::net::{TcpListener, ToSocketAddrs};
use tracing::debug;
use crate::network::Connection;

pub struct GameServer {
  listener: TcpListener
}

impl GameServer {
  pub async fn new<A: ToSocketAddrs>(address: A) -> Result<Self> {
    Ok(Self {
      listener: TcpListener::bind(address).await?
    })
  }

  pub fn poll_accept(&self, cx: &mut Context) -> Poll<Result<Connection>> {
    match self.listener.poll_accept(cx) {
      Poll::Pending => Poll::Pending,
      Poll::Ready(Ok((socket, address))) => {
        debug!("accepted socket from {:?}", address);

        let connection = Connection::new(Box::new(socket));
        Poll::Ready(Ok(connection))
      },
      Poll::Ready(Err(error)) => Poll::Ready(Err(anyhow!(error)))
    }
  }
}

impl Stream for GameServer {
  type Item = Result<Connection>;

  fn poll_next(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
    match self.poll_accept(cx) {
      Poll::Pending => Poll::Pending,
      Poll::Ready(Ok(connection)) => Poll::Ready(Some(Ok(connection))),
      Poll::Ready(Err(error)) => Poll::Ready(Some(Err(error)))
    }
  }
}
