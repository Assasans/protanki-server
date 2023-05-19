use std::io::{Write, Read};
use byteorder::{BigEndian, WriteBytesExt, ReadBytesExt};

use crate::codec::{Codec, CodecResult};

macro_rules! enum_impl {
  ($name:ident { $($variant:ident: $value:expr)* }) => {
    #[derive(Clone, Copy, Debug, PartialEq, Eq)]
    pub enum $name {
      Undefined = -1,
      $($variant = $value),*
    }

    impl Default for $name {
      fn default() -> Self {
        $name::Undefined
      }
    }

    impl From<i32> for $name {
      fn from(value: i32) -> Self {
        match value {
          $($value => $name::$variant,)*
          _ => todo!("unknown enum value: {value}")
        }
      }
    }

    impl From<$name> for i32 {
      fn from(value: $name) -> Self {
        match value {
          $($name::$variant => $value,)*
          $name::Undefined => todo!("undefined enum value: {value:?}")
        }
      }
    }

    impl Codec for $name {
      fn encode<W: Write + ?Sized>(&self, writer: &mut W) -> CodecResult<()> {
        Ok(writer.write_i32::<BigEndian>((*self).into())?)
      }

      fn decode<R: Read + ?Sized>(&mut self, reader: &mut R) -> CodecResult<()> {
        *self = reader.read_i32::<BigEndian>()?.into();
        Ok(())
      }
    }
  };
}

enum_impl!(ValidationStatus {
  TooShort: 0
  TooLong: 1
  NotUnique: 2
  NotMatchPattern: 3
  Forbidden: 4
  Correct: 5
});

enum_impl!(MapTheme {
  Summer: 0
  Winter: 1
  Space: 2
  SummerDay: 3
  SummerNight: 4
  WinterDay: 5
});

enum_impl!(LayoutState {
  BattleSelect: 0
  Garage: 1
  Payment: 2
  Battle: 3
  ReloadSpace: 4
});

enum_impl!(ItemViewCategory {
  Weapon: 0
  Armor: 1
  Paint: 2
  Inventory: 3
  Kit: 4
  Special: 5
  GivenPresents: 6
});

enum_impl!(ItemCategory {
  Weapon: 0
  Armor: 1
  Color: 2
  Inventory: 3
  Plugin: 4
  Kit: 5
  Emblem: 6
  Present: 7
  GivenPresent: 8
});

enum_impl!(IsisState {
  Off: 0
  Idle: 1
  Healing: 2
  Damaging: 3
});

enum_impl!(EquipmentConstraintsMode {
  None: 0
  HornetRailgun: 1
  WaspRailgun: 2
  HornetWaspRailgun: 3
});

enum_impl!(DamageIndicatorType {
  Normal: 0
  Critical: 1
  Fatal: 2
  Heal: 3
});

enum_impl!(ControlPointState {
  Red: 0
  Blue: 1
  Neutral: 2
});

enum_impl!(ChatModeratorLevel {
  None: 0
  CommunityManager: 1
  Administrator: 2
  Moderator: 3
  Candidate: 4
});

enum_impl!(BattleTeam {
  Red: 0
  Blue: 1
  None: 2
});

enum_impl!(BattleSuspicionLevel {
  None: 0
  Low: 1
  High: 2
});

enum_impl!(BattleMode {
  Dm: 0
  Tdm: 1
  Ctf: 2
  Cp: 3
  As: 4
});

enum_impl!(Achievement {
  FirstRankUp: 0
  FirstPurchase: 1
  SetEmail: 2
  FightFirstBattle: 3
  FirstDonate: 4
});

enum_impl!(CaptchaLocation {
  LoginForm: 0
  RegisterForm: 1
  ClientStartup: 2
  RestorePasswordForm: 3
  EmailChangeHash: 4
  AccountSettingsForm: 5
});
