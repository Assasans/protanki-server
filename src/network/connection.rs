use std::{
  io::{Cursor, Read, Write},
  fmt::{Debug, Formatter},
  pin::Pin,
  task::{Context, Poll, Waker}
};
use byteorder::{BigEndian, ReadBytesExt, WriteBytesExt};
use futures_util::Stream;
use tracing::{error, trace, warn};

use protocol::{
  packet::{Packet, PacketDowncast, PacketRegistry, UnknownPacket},
  crypto::{CryptoContext, EmptyCryptoContext, XorCryptoContext, XorCryptoMode}
};
use crate::network::{ConnectionError, ConnectionResult};
use super::Socket;

#[derive(Debug, PartialEq, Eq, Clone, Copy)]
pub enum ConnectionState {
  Disconnected,
  Connected
}

pub struct Connection {
  socket: Box<dyn Socket>,
  pub(crate) crypto_context: Box<dyn CryptoContext>,

  state: ConnectionState,

  recv_buffer: Vec<u8>,
  recv_buffer_index: usize,

  send_buffer: Vec<u8>,
  send_waker: Option<Waker>,

  packet_registry: PacketRegistry
}

impl Debug for Connection {
  fn fmt(&self, formatter: &mut Formatter<'_>) -> std::fmt::Result {
    formatter.debug_struct("Connection")
             .field("socket", &format!(
               "Socket {{ peer_addr: {:?}, local_addr: {:?} }}",
               self.socket.peer_addr(),
               self.socket.local_addr()
             ))
             .field("crypto_context", &self.crypto_context)
             .field("state", &self.state)
             .finish()
  }
}

const RECV_BUFFER: usize = 16 * 1024;
const RECV_BUFFER_TAIL: usize = 1024;

impl Connection {
  pub fn new(socket: Box<dyn Socket>) -> Self {
    Self {
      socket,
      crypto_context: Box::new(EmptyCryptoContext::new()),

      state: ConnectionState::Connected,

      recv_buffer: Vec::with_capacity(RECV_BUFFER),
      recv_buffer_index: 0,

      send_buffer: Vec::with_capacity(RECV_BUFFER),
      send_waker: None,

      packet_registry: PacketRegistry::new()
    }
  }

  pub fn send(&mut self, packet: &dyn Packet) -> ConnectionResult<()> {
    let mut buffer = Vec::with_capacity(8);
    let mut cursor = Cursor::new(&mut buffer);

    cursor.set_position(4);
    cursor.write_i32::<BigEndian>(packet.packet_id()).map_err(|error| ConnectionError::SendError(Box::new(error)))?;

    if let Some(packet) = packet.downcast_ref::<UnknownPacket>() {
      cursor.write_all(packet.payload()).map_err(|error| ConnectionError::SendError(Box::new(error)))?;
    } else {
      self.packet_registry.encode(&mut cursor, packet).map_err(|error| ConnectionError::SendError(Box::new(error)))?;
    }
    let packet_length = cursor.position() as usize;

    cursor.set_position(0);
    cursor.write_u32::<BigEndian>(packet_length as u32).map_err(|error| ConnectionError::SendError(Box::new(error)))?;

    self.crypto_context.encrypt(&mut buffer[8..]).map_err(|error| ConnectionError::SendError(Box::new(error)))?;

    self.send_buffer.append(&mut buffer);
    // Notify the IO task about a new packet
    if let Some(waker) = self.send_waker.take() {
      waker.wake();
    }

    trace!("> {: >11} {: >2} {:?} ({} bytes)", packet.packet_id(), packet.model_id(), packet, packet_length - 8);

    Ok(())
  }

  fn try_parse_read_buffer(&mut self) -> Poll<ConnectionResult<Box<dyn Packet>>> {
    if self.recv_buffer_index < 8 {
      return Poll::Pending
    }

    let mut packet_reader = Cursor::new(&self.recv_buffer[0..self.recv_buffer_index]);

    let packet_length = packet_reader.read_u32::<BigEndian>().map_err(|error| ConnectionError::RecvError(Box::new(error)))? as usize;
    if packet_length < 8 || packet_length > 1024 * 64 {
      return Poll::Ready(Err(ConnectionError::InvalidPacketSize(packet_length)));
    }

    if self.recv_buffer_index < packet_length {
      if self.recv_buffer.len() < packet_length {
        self.recv_buffer.resize(packet_length, 0);
      }

      return Poll::Pending;
    }

    let packet_id = packet_reader.read_i32::<BigEndian>().map_err(|error| ConnectionError::RecvError(Box::new(error)))?;

    let payload_offset = packet_reader.position() as usize;
    let mut payload = &mut self.recv_buffer[payload_offset..packet_length];
    self.crypto_context.decrypt(&mut payload).map_err(|error| ConnectionError::RecvError(Box::new(error)))?;

    let mut payload_reader = Cursor::new(payload);

    let packet = match self.packet_registry.decode(&mut payload_reader, packet_id) {
      Ok(Some(packet)) => packet,
      Ok(None) => {
        let mut buffer = Vec::with_capacity(packet_length - 8);
        payload_reader.read_to_end(&mut buffer).map_err(|error| ConnectionError::RecvError(Box::new(error)))?;

        Box::new(UnknownPacket::new(packet_id, buffer))
      },
      Err(error) => {
        error!("failed to decode packet {packet_id}");
        return Poll::Ready(Err(ConnectionError::DecodeError(Box::new(error))))
      }
    };

    let remaining = payload_reader.position().min(payload_reader.get_ref().len() as u64);
    let slice = &payload_reader.get_ref()[remaining as usize..];
    if !slice.is_empty() {
      warn!("Packet decoder did not read whole packet of id {} ({} bytes left).", packet_id, slice.len());
    }

    trace!("< {: >11} {: >2} {:?} ({} bytes)", packet.packet_id(), packet.model_id(), packet, packet_length - 8);

    self.recv_buffer.copy_within(packet_length.., 0);
    self.recv_buffer_index -= packet_length;

    Poll::Ready(Ok(packet))
  }

  fn poll_recv(&mut self, cx: &mut Context) -> Poll<ConnectionResult<Box<dyn Packet>>> {
    // Keep at least [RECV_BUFFER] bytes available in the receive buffer
    if self.recv_buffer.len() - self.recv_buffer_index < RECV_BUFFER_TAIL {
      self.recv_buffer.resize(self.recv_buffer.len() + RECV_BUFFER_TAIL, 0);
    }

    loop {
      match self.try_parse_read_buffer() {
        Poll::Pending => {},
        Poll::Ready(Ok(packet)) => return Poll::Ready(Ok(packet)),
        Poll::Ready(Err(error)) => return Poll::Ready(Err(error))
      }

      match self.socket.poll_recv(cx, &mut self.recv_buffer[self.recv_buffer_index..]) {
        Poll::Pending => return Poll::Pending,
        Poll::Ready(Ok(length)) => {
          if length == 0 {
            return Poll::Ready(Err(ConnectionError::SocketEndOfFile));
          }

          self.recv_buffer_index += length;
        },
        Poll::Ready(Err(error)) => return Poll::Ready(Err(ConnectionError::RecvError(Box::new(error))))
      }
    }
  }

  fn poll_send(&mut self, cx: &mut Context) -> Poll<ConnectionResult<Box<dyn Packet>>> {
    self.send_waker.replace(cx.waker().clone());

    while !self.send_buffer.is_empty() {
      match self.socket.poll_send(cx, &self.send_buffer) {
        Poll::Ready(Ok(length)) => {
          // Move rest of data to the start of the buffer
          self.send_buffer.copy_within(length.., 0);
          self.send_buffer.truncate(self.send_buffer.len() - length);
        },
        Poll::Ready(Err(error)) => return Poll::Ready(Err(ConnectionError::SendError(Box::new(error)))),
        Poll::Pending => return Poll::Pending
      }
    }

    Poll::Pending
  }

  fn poll_io(&mut self, cx: &mut Context) -> Poll<ConnectionResult<Box<dyn Packet>>> {
    match self.poll_send(cx) {
      Poll::Pending => {},
      Poll::Ready(item) => return Poll::Ready(item)
    }

    match self.poll_recv(cx) {
      Poll::Pending => {},
      Poll::Ready(item) => return Poll::Ready(item)
    }

    Poll::Pending
  }

  pub fn init_crypto(&mut self) -> ConnectionResult<()> {
    let key = XorCryptoContext::generate_key();
    self.crypto_context = Box::new(XorCryptoContext::new(XorCryptoMode::Server, &key));

    Ok(())
  }
}

impl Stream for Connection {
  type Item = ConnectionResult<Box<dyn Packet>>;

  fn poll_next(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
    if self.state == ConnectionState::Disconnected {
      return Poll::Ready(None);
    }

    match self.poll_io(cx) {
      Poll::Ready(Ok(item)) => Poll::Ready(Some(Ok(item))),
      Poll::Ready(Err(error)) => {
        error!("connection error: {:?}", error);
        self.state = ConnectionState::Disconnected;

        Poll::Ready(Some(Err(error)))
      },
      Poll::Pending => Poll::Pending
    }
  }
}
