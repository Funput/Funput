use std::io;

pub(super) struct Cursor<'a> {
    bytes: &'a [u8],
    position: usize,
}

impl<'a> Cursor<'a> {
    pub(super) fn new(bytes: &'a [u8]) -> Self {
        Self { bytes, position: 0 }
    }

    pub(super) fn take(&mut self, len: usize) -> io::Result<&'a [u8]> {
        let end = self.position.checked_add(len).ok_or_else(invalid_data)?;
        let value = self
            .bytes
            .get(self.position..end)
            .ok_or_else(invalid_data)?;
        self.position = end;
        Ok(value)
    }

    pub(super) fn u16(&mut self) -> io::Result<u16> {
        Ok(u16::from_le_bytes(
            self.take(2)?.try_into().map_err(|_| invalid_data())?,
        ))
    }

    pub(super) fn u32(&mut self) -> io::Result<u32> {
        Ok(u32::from_le_bytes(
            self.take(4)?.try_into().map_err(|_| invalid_data())?,
        ))
    }

    pub(super) fn u64(&mut self) -> io::Result<u64> {
        Ok(u64::from_le_bytes(
            self.take(8)?.try_into().map_err(|_| invalid_data())?,
        ))
    }

    pub(super) fn remaining(&self) -> usize {
        self.bytes.len() - self.position
    }

    pub(super) fn is_empty(&self) -> bool {
        self.position == self.bytes.len()
    }
}

pub(super) fn put_u16(out: &mut Vec<u8>, value: u16) {
    out.extend_from_slice(&value.to_le_bytes());
}

pub(super) fn put_u32(out: &mut Vec<u8>, value: u32) {
    out.extend_from_slice(&value.to_le_bytes());
}

pub(super) fn put_u64(out: &mut Vec<u8>, value: u64) {
    out.extend_from_slice(&value.to_le_bytes());
}

pub(crate) fn checksum(bytes: &[u8]) -> u32 {
    let mut crc = u32::MAX;
    for byte in bytes {
        crc ^= u32::from(*byte);
        for _ in 0..8 {
            let mask = 0u32.wrapping_sub(crc & 1);
            crc = (crc >> 1) ^ (0xEDB8_8320 & mask);
        }
    }
    !crc
}

pub(super) fn invalid_data() -> io::Error {
    io::Error::new(io::ErrorKind::InvalidData, "invalid personal lexicon data")
}
