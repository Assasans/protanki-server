macro_rules! assert_decode_eq_encode {
  ($codec:expr, $registry:expr, $value:expr) => {{
    use crate::codec::Codec;

    let mut encoded = Vec::with_capacity(16);

    $codec.encode(&$registry, &mut encoded, &$value).unwrap();
    let decoded = $codec.decode(&$registry, &mut std::io::Cursor::new(encoded)).unwrap();

    assert_eq!(decoded, $value);
  }};
}
pub(crate) use assert_decode_eq_encode;

macro_rules! impl_test_decode_eq_encode {
  ($type:ty, $codec:expr) => {
    paste::paste! {
      #[test]
      fn [<$type _decode_eq_encode>]() {
        use crate::codec::CodecRegistry;
        use crate::codec::test::assert_decode_eq_encode;

        let registry = CodecRegistry::new();

        assert_decode_eq_encode!($codec, registry, $type::default());
        assert_decode_eq_encode!($codec, registry, $type::MIN);
        assert_decode_eq_encode!($codec, registry, $type::MAX);
      }
    }
  };
}
pub(crate) use impl_test_decode_eq_encode;
