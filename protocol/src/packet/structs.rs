use std::io::{Write, Read};

use crate::{
  codec::{Codec, CodecRegistry, CodecRegistryExt, CodecResult, codecs::VectorCodec},
  packet::enums::*
};

macro_rules! struct_impl {
  ($name:ident { $($field:ident: $codec:ty)* }) => {
    #[derive(Default, Clone, Debug)]
    pub struct $name {
      $(pub $field: $codec),*
    }

    impl Codec for $name {
      type Target = Self;

      fn encode(&self, registry: &CodecRegistry, writer: &mut dyn Write, value: &Self::Target) -> CodecResult<()> {
        $(registry.encode(writer, &value.$field)?;)*
        Ok(())
      }

      fn decode(&self, registry: &CodecRegistry, reader: &mut dyn Read) -> CodecResult<Self::Target> {
        let mut value = Self::Target::default();
        $(value.$field = registry.decode(reader)?;)*
        Ok(value)
      }
    }
  };
}

pub(crate) fn register_structs(registry: &mut CodecRegistry) {
  registry.register_codec(class_16::default());
  registry.register_codec(class_14::default());
  registry.register_codec(WeeklyQuestRewardItem::default());
  registry.register_codec(WeeklyQuestDescription::default());
  registry.register_codec(Vector3d::default());
  registry.register_codec(UserStatus::default());
  registry.register_codec(UserStat::default());
  registry.register_codec(UserReward::default());
  registry.register_codec(UserPropertyCC::default());
  registry.register_codec(UserInfo::default());
  registry.register_codec(UserContainerCC::default());
  registry.register_codec(TipItemCC::default());
  registry.register_codec(TargetTankDamage::default());
  registry.register_codec(TargetPosition::default());
  registry.register_codec(TargetHit::default());
  registry.register_codec(StringPair::default());
  registry.register_codec(StatisticsTeamCC::default());
  registry.register_codec(StatisticsModelCC::default());
  registry.register_codec(StatisticsDMCC::default());
  registry.register_codec(SocialNetworkPanelParams::default());
  registry.register_codec(SocialNetworkPanelCC::default());
  registry.register_codec(RotateTurretCommand::default());
  registry.register_codec(RankNotifierData::default());
  registry.register_codec(Range::default());
  registry.register_codec(PremiumNotifierData::default());
  registry.register_codec(PremiumNotifierCC::default());
  registry.register_codec(PremiumAccountAlertCC::default());
  registry.register_codec(OnlineNotifierData::default());
  registry.register_codec(NewsShowingCC::default());
  registry.register_codec(NewsItemCC::default());
  registry.register_codec(MoveCommand::default());
  registry.register_codec(LocaleStruct::default());
  registry.register_codec(ImagePair::default());
  registry.register_codec(GarageItemInfo::default());
  registry.register_codec(DominationSounds::default());
  registry.register_codec(DominationResources::default());
  registry.register_codec(DailyQuestPrizeInfo::default());
  registry.register_codec(DailyQuestInfo::default());
  registry.register_codec(ControlPointsCC::default());
  registry.register_codec(ClientPointData::default());
  registry.register_codec(ClientFlag::default());
  registry.register_codec(ClientAssaultFlag::default());
  registry.register_codec(ChatMessage::default());
  registry.register_codec(ChatCC::default());
  registry.register_codec(CaptureTheFlagSoundFX::default());
  registry.register_codec(CaptureTheFlagCC::default());
  registry.register_codec(BonusInfoCC::default());
  registry.register_codec(BattleNotifierData::default());
  registry.register_codec(BattleMineCC::default());
  registry.register_codec(BattleMine::default());
  registry.register_codec(BattleLimits::default());
  registry.register_codec(BattleInviteMessage::default());
  registry.register_codec(BattleInviteCC::default());
  registry.register_codec(BattleInfoUser::default());
  registry.register_codec(BattleInfoData::default());
  registry.register_codec(BattleCreateParameters::default());
  registry.register_codec(AssaultSoundFX::default());
  registry.register_codec(AssaultCC::default());
  registry.register_codec(AssaultBase::default());
  registry.register_codec(AchievementCC::default());

  /* Vector */
  registry.register_codec(VectorCodec::<DamageIndicatorType>::default());
  registry.register_codec(VectorCodec::<WeeklyQuestRewardItem>::default());
  registry.register_codec(VectorCodec::<Vector3d>::default());
  registry.register_codec(VectorCodec::<UserStat>::default());
  registry.register_codec(VectorCodec::<UserReward>::default());
  registry.register_codec(VectorCodec::<UserInfo>::default());
  registry.register_codec(VectorCodec::<TargetTankDamage>::default());
  registry.register_codec(VectorCodec::<TargetPosition>::default());
  registry.register_codec(VectorCodec::<TargetHit>::default());
  registry.register_codec(VectorCodec::<StringPair>::default());
  registry.register_codec(VectorCodec::<SocialNetworkPanelParams>::default());
  registry.register_codec(VectorCodec::<NewsItemCC>::default());
  registry.register_codec(VectorCodec::<ImagePair>::default());
  registry.register_codec(VectorCodec::<GarageItemInfo>::default());
  registry.register_codec(VectorCodec::<DailyQuestPrizeInfo>::default());
  registry.register_codec(VectorCodec::<DailyQuestInfo>::default());
  registry.register_codec(VectorCodec::<ClientPointData>::default());
  registry.register_codec(VectorCodec::<ClientAssaultFlag>::default());
  registry.register_codec(VectorCodec::<ChatMessage>::default());
  registry.register_codec(VectorCodec::<CaptchaLocation>::default());
  registry.register_codec(VectorCodec::<BattleMine>::default());
  registry.register_codec(VectorCodec::<AssaultBase>::default());
  registry.register_codec(VectorCodec::<Achievement>::default());
}

pub type ResourceId = i32;
pub type BonusSpawnData = String;

struct_impl!(class_16 {
  var_497: String
  var_180: String
});

struct_impl!(class_14 {
  var_49: i32
  var_850: String
  var_640: i32
  var_2587: Vector3d
});

struct_impl!(WeeklyQuestRewardItem {
  var_809: i32
  var_259: ResourceId
});

struct_impl!(WeeklyQuestDescription {
  var_2401: i32
  var_1080: i32
  var_1186: bool
  var_2313: ResourceId
  var_2774: ResourceId
});

struct_impl!(Vector3d {
  var_2927: f32
  var_2778: f32
  var_462: f32
});

struct_impl!(UserStatus {
  var_452: ChatModeratorLevel
  var_1057: String
  var_1500: i32
  var_497: String
  var_180: String
});

struct_impl!(UserStat {
  var_25: i32
  var_2296: i32
  var_135: i32
  var_2452: String
});

struct_impl!(UserReward {
  var_2457: i32
  var_471: i32
  var_964: i32
  var_180: String
});

struct_impl!(UserPropertyCC {
  var_1242: i32
  var_2611: i32
  var_1530: i32
  var_2734: bool
  var_400: i32
  var_1626: i32
  var_2673: i32
  var_1: i8
  var_2062: f32
  var_135: i32
  var_112: i32
  var_497: String
  var_1091: String
});

struct_impl!(UserInfo {
  var_452: ChatModeratorLevel
  var_25: i32
  var_2296: i32
  var_1: i8
  var_135: i32
  var_497: String
});

struct_impl!(UserContainerCC {
  var_288: Vec<String>
});

struct_impl!(TipItemCC {
  var_740: ResourceId
});

struct_impl!(TargetTankDamage {
  var_106: f32
  var_1157: DamageIndicatorType
  var_492: String
});

struct_impl!(TargetPosition {
  var_1261: Vector3d
  var_2126: Vector3d
  var_651: Vector3d
  var_492: String
  var_2013: f32
});

struct_impl!(TargetHit {
  var_2440: Vector3d
  var_131: Vector3d
  var_1294: i8
  var_492: String
});

struct_impl!(StringPair {
  var_525: String
  var_34: String
});

struct_impl!(StatisticsTeamCC {
  var_1934: i32
  var_1433: i32
  var_1385: Vec<UserInfo>
  var_667: Vec<UserInfo>
});

struct_impl!(StatisticsModelCC {
  var_1062: BattleMode
  var_767: EquipmentConstraintsMode
  var_1680: i32
  var_460: BattleLimits
  var_630: String
  var_1782: bool
  var_232: i32
  var_563: String
  var_455: i32
  var_2285: bool
  var_1394: i32
  var_1360: bool
  var_296: Vec<String>
  var_2039: i32
  var_1922: bool
});

struct_impl!(StatisticsDMCC {
  var_233: Vec<UserInfo>
});

struct_impl!(SocialNetworkPanelParams {
  var_88: String
  var_2074: bool
  var_764: String
});

struct_impl!(SocialNetworkPanelCC {
  var_272: bool
  var_1754: Vec<SocialNetworkPanelParams>
});

struct_impl!(RotateTurretCommand {
  var_2473: f32
  var_1117: i8
});

struct_impl!(RankNotifierData {
  var_1: i32
  var_180: String
});

struct_impl!(Range {
  var_2793: i32
  var_1078: i32
});

struct_impl!(PremiumNotifierData {
  var_2535: i32
  var_180: String
});

struct_impl!(PremiumNotifierCC {
  var_1928: i32
});

struct_impl!(PremiumAccountAlertCC {
  var_1028: bool
  var_702: bool
  var_674: f32
  var_2092: bool
  var_530: bool
});

struct_impl!(OnlineNotifierData {
  var_2304: bool
  var_112: i32
  var_180: String
});

struct_impl!(NewsShowingCC {
  var_261: Vec<NewsItemCC>
});

struct_impl!(NewsItemCC {
  var_2660: String
  var_2162: String
  var_1569: String
});

struct_impl!(MoveCommand {
  var_723: Vector3d
  var_1117: i8
  var_1727: Vector3d
  var_2126: Vector3d
  var_651: Vector3d
});

struct_impl!(LocaleStruct {
  var_258: Vec<ImagePair>
  var_1518: Vec<StringPair>
});

struct_impl!(ImagePair {
  var_525: String
  var_34: Vec<u8>
});

struct_impl!(GarageItemInfo {
  var_1535: ItemCategory
  var_584: ItemViewCategory
  var_1275: i32
  var_2795: bool
  var_491: String
  var_651: i32
  var_602: bool
  var_740: ResourceId
  var_885: i32
});

struct_impl!(DominationSounds {
  var_1491: ResourceId
  var_1233: ResourceId
  var_2093: ResourceId
  var_1892: ResourceId
  var_2722: ResourceId
  var_2749: ResourceId
  var_1291: ResourceId
  var_1075: ResourceId
  var_592: ResourceId
  var_1041: ResourceId
});

struct_impl!(DominationResources {
  var_115: ResourceId
  var_626: ResourceId
  var_157: ResourceId
  var_1329: ResourceId
  var_1734: ResourceId
  var_1098: ResourceId
  var_1856: ResourceId
  var_2551: ResourceId
  var_2324: ResourceId
  var_319: ResourceId
  var_2651: ResourceId
  var_652: ResourceId
});

struct_impl!(DailyQuestPrizeInfo {
  var_809: i32
  var_491: String
});

struct_impl!(DailyQuestInfo {
  var_2342: bool
  var_486: String
  var_1788: i32
  var_730: ResourceId
  var_2191: Vec<DailyQuestPrizeInfo>
  var_1025: i32
  var_1596: i32
  var_1676: i32
});

struct_impl!(ControlPointsCC {
  var_1014: f32
  var_113: f32
  var_825: f32
  var_1163: Vec<ClientPointData>
  var_1015: DominationResources
  var_1640: DominationSounds
});

struct_impl!(ClientPointData {
  var_400: i32
  var_491: String
  var_651: Vector3d
  var_135: f32
  var_1885: f32
  var_2158: ControlPointState
  var_1322: Vec<String>
});

struct_impl!(ClientFlag {
  var_788: Vector3d
  var_1589: String
  var_831: Vector3d
});

struct_impl!(ClientAssaultFlag {
  var_788: Vector3d
  var_1589: String
  var_831: Vector3d
  var_400: i32
});

struct_impl!(ChatMessage {
  var_1946: UserStatus
  var_1415: bool
  var_2723: UserStatus
  var_175: String
  var_1905: bool
});

struct_impl!(ChatCC {
  var_1639: bool
  var_1340: bool
  var_2171: i32
  var_2250: bool
  var_452: ChatModeratorLevel
  var_1337: String
  var_2068: Vec<String>
  var_1183: i32
  var_2507: i32
  var_1138: String
  var_783: bool
  var_963: bool
});

struct_impl!(CaptureTheFlagSoundFX {
  var_442: ResourceId
  var_420: ResourceId
  var_1994: ResourceId
  var_1503: ResourceId
  var_920: ResourceId
  var_559: ResourceId
  var_865: ResourceId
  var_930: ResourceId
});

struct_impl!(CaptureTheFlagCC {
  var_1831: ClientFlag
  var_217: ResourceId
  var_2542: ResourceId
  var_760: ResourceId
  var_271: ClientFlag
  var_934: ResourceId
  var_2821: ResourceId
  var_989: ResourceId
  var_1640: CaptureTheFlagSoundFX
});

struct_impl!(BonusInfoCC {
  var_2591: String
  var_730: ResourceId
  var_1147: String
});

struct_impl!(BattleNotifierData {
  var_4: BattleInfoData
  var_180: String
});

struct_impl!(BattleMineCC {
  var_833: ResourceId
  var_1768: i32
  var_881: Vec<BattleMine>
  var_1781: ResourceId
  var_177: ResourceId
  var_476: ResourceId
  var_1608: ResourceId
  var_2293: ResourceId
  var_55: f32
  var_502: ResourceId
  var_1763: ResourceId
  var_2332: f32
  var_1485: ResourceId
  var_1684: f32
  var_1100: ResourceId
  var_899: f32
  var_2827: f32
  var_797: ResourceId
});

struct_impl!(BattleMine {
  var_1154: bool
  var_522: String
  var_1787: String
  var_651: Vector3d
});

struct_impl!(BattleLimits {
  var_978: i32
  var_2918: i32
});

struct_impl!(BattleInviteMessage {
  var_2139: bool
  var_1523: bool
  var_2258: String
  var_1258: String
  var_630: String
  var_2089: BattleMode
  var_1913: bool
  var_501: bool
  var_518: bool
  var_112: i32
  var_485: String
});

struct_impl!(BattleInviteCC {
  var_429: ResourceId
});

struct_impl!(BattleInfoUser {
  var_2296: i32
  var_135: i32
  var_1414: bool
  var_2452: String
});

struct_impl!(BattleInfoData {
  var_2258: String
  var_630: String
  var_2089: BattleMode
  var_501: bool
  var_2437: bool
  var_1986: Range
  var_518: bool
  var_112: i32
});

struct_impl!(BattleCreateParameters {
  var_811: bool
  var_1062: BattleMode
  var_2200: bool
  var_767: EquipmentConstraintsMode
  var_2415: bool
  var_460: BattleLimits
  var_114: String
  var_232: i32
  var_491: String
  var_2285: bool
  var_501: bool
  var_2437: bool
  var_1289: Range
  var_2065: bool
  var_1476: MapTheme
  var_613: bool
  var_2499: bool
  var_495: bool
  var_2674: bool
});

struct_impl!(AssaultSoundFX {
  var_420: ResourceId
  var_1503: ResourceId
  var_559: ResourceId
  var_930: ResourceId
});

struct_impl!(AssaultCC {
  var_2402: Vec<ClientAssaultFlag>
  var_2659: ResourceId
  var_2833: ResourceId
  var_521: ResourceId
  var_603: ResourceId
  var_1283: ResourceId
  var_1687: Vec<AssaultBase>
  var_1640: AssaultSoundFX
});

struct_impl!(AssaultBase {
  var_400: i32
  var_651: Vector3d
});

struct_impl!(AchievementCC {
  var_2763: Vec<Achievement>
});
