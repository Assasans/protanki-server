use std::net::SocketAddr;
use std::task::{Context, Poll};
use tokio::io;
use tokio::net::TcpStream;

pub trait Socket: Send + Sync {
  fn poll_recv(&mut self, cx: &mut Context, buffer: &mut [u8]) -> Poll<io::Result<usize>>;
  fn poll_send(&mut self, cx: &mut Context, buffer: &[u8]) -> Poll<io::Result<usize>>;

  fn peer_addr(&self) -> io::Result<SocketAddr>;
  fn local_addr(&self) -> io::Result<SocketAddr>;
}

macro_rules! poll_ready_passthrough {
  ($poll:expr) => {
    match $poll {
      Poll::Pending => return Poll::Pending,
      Poll::Ready(Ok(_)) => {},
      Poll::Ready(Err(error)) => return Poll::Ready(Err(error))
    }
  };
}

macro_rules! poll_io_passthrough {
  ($poll:expr) => {
    match $poll {
      Ok(length) => return Poll::Ready(Ok(length)),
      Err(error) if error.kind() == io::ErrorKind::WouldBlock => continue,
      Err(error) => return Poll::Ready(Err(error))
    }
  };
}

impl Socket for TcpStream {
  fn poll_recv(&mut self, cx: &mut Context, buffer: &mut [u8]) -> Poll<io::Result<usize>> {
    loop {
      poll_ready_passthrough!(self.poll_read_ready(cx));
      poll_io_passthrough!(self.try_read(buffer));
    }
  }

  fn poll_send(&mut self, cx: &mut Context, buffer: &[u8]) -> Poll<io::Result<usize>> {
    loop {
      poll_ready_passthrough!(self.poll_write_ready(cx));
      poll_io_passthrough!(self.try_write(buffer));
    }
  }

  fn local_addr(&self) -> io::Result<SocketAddr> {
    TcpStream::local_addr(self)
  }

  fn peer_addr(&self) -> io::Result<SocketAddr> {
    TcpStream::peer_addr(self)
  }
}
