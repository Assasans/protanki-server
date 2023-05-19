use std::io::{Write, Read};

use crate::{
  codec::{Codec, CodecResult},
  packet::enums::*
};

macro_rules! struct_impl {
  ($name:ident { $($field:ident: $codec:ty)* }) => {
    #[derive(Default, Clone, Debug)]
    pub struct $name {
      $(pub $field: $codec),*
    }

    impl Codec for $name {
      fn encode<W: Write + ?Sized>(&self, writer: &mut W) -> CodecResult<()> {
        $(self.$field.encode(writer)?;)*
        Ok(())
      }

      fn decode<R: Read + ?Sized>(&mut self, reader: &mut R) -> CodecResult<()> {
        $(self.$field.decode(reader)?;)*
        Ok(())
      }
    }
  };
}

pub type ResourceId = i32;
pub type BonusSpawnData = Option<String>;

struct_impl!(class_16 {
  uid: Option<String>
  user_id: Option<String>
});

struct_impl!(class_14 {
  bonus_id: Option<String>
  name_38: i32
  method_219: Option<Vector3d>
});

struct_impl!(WeeklyQuestRewardItem {
  count: i32
  method_2511: ResourceId
});

struct_impl!(WeeklyQuestDescription {
  method_2704: i32
  name_23: i32
  method_798: bool
  name_35: ResourceId
  name_61: ResourceId
});

struct_impl!(Vector3d {
  x: f32
  y: f32
  z: f32
});

struct_impl!(UserStatus {
  chat_moderator_level: ChatModeratorLevel
  ip: Option<String>
  rank_index: i32
  uid: Option<String>
});

struct_impl!(UserStat {
  deaths: i32
  kills: i32
  score: i32
  user: Option<String>
});

struct_impl!(UserReward {
  name_6: i32
  name_59: i32
  reward: i32
  user_id: Option<String>
});

struct_impl!(UserPropertyCC {
  crystals: i32
  current_rank_score: i32
  duration_crystal_abonement: i32
  has_double_crystal: bool
  next_rank_score: i32
  place: i32
  rank: i8
  rating: f32
  score: i32
  server_number: i32
  id: Option<String>
  user_profile_url: Option<String>
});

struct_impl!(UserInfo {
  chat_moderator_level: ChatModeratorLevel
  deaths: i32
  kills: i32
  rank: i8
  score: i32
  uid: Option<String>
});

struct_impl!(UserContainerCC {
  users: Option<Vec<Option<String>>>
});

struct_impl!(TipItemCC {
  preview: ResourceId
});

struct_impl!(TargetTankDamage {
  method_2673: f32
  method_2351: DamageIndicatorType
  target: Option<String>
});

struct_impl!(TargetPosition {
  name_22: Option<Vector3d>
  orientation: Option<Vector3d>
  position: Option<Vector3d>
  turret_angle: f32
});

struct_impl!(TargetHit {
  direction: Option<Vector3d>
  name_22: Option<Vector3d>
  method_1131: i8
});

struct_impl!(StringPair {
  key: Option<String>
  value: Option<String>
});

struct_impl!(StatisticsTeamCC {
  method_1860: i32
  method_2648: i32
  method_1840: Vec<UserInfo>
  method_1572: Vec<UserInfo>
});

struct_impl!(StatisticsModelCC {
  battle_mode: BattleMode
  equipment_constraints_mode: EquipmentConstraintsMode
  fund: i32
  method_1309: BattleLimits
  map_name: Option<String>
  max_people_count: i32
  parkour_mode: bool
  method_2682: i32
  spectator: bool
  method_2378: Option<Vec<Option<String>>>
  name_5: i32
});

struct_impl!(StatisticsDMCC {
  users_info: Vec<UserInfo>
});

struct_impl!(SocialNetworkPanelParams {
  authorization_url: Option<String>
  link_exists: bool
  sn_id: Option<String>
});

struct_impl!(SocialNetworkPanelCC {
  password_created: bool
  social_network_params: Vec<SocialNetworkPanelParams>
});

struct_impl!(RotateTurretCommand {
  angle: f32
  control: i8
});

struct_impl!(RankNotifierData {
  rank: i32
  user_id: Option<String>
});

struct_impl!(Range {
  max: i32
  min: i32
});

struct_impl!(PremiumNotifierData {
  premium_time_left_in_seconds: i32
  user_id: Option<String>
});

struct_impl!(PremiumNotifierCC {
  life_time_in_seconds: i32
});

struct_impl!(PremiumAccountAlertCC {
  need_show_notification_completion_premium: bool
  need_show_welcome_alert: bool
  reminder_completion_premium_time: f32
  was_show_alert_for_first_purchase_premium: bool
  was_show_reminder_completion_premium: bool
});

struct_impl!(OnlineNotifierData {
  online: bool
  server_number: i32
  user_id: Option<String>
});

struct_impl!(NewsShowingCC {
  news_items: Vec<NewsItemCC>
});

struct_impl!(NewsItemCC {
  image_url: Option<String>
  news_date: Option<String>
  news_text: Option<String>
});

struct_impl!(MoveCommand {
  angular_velocity: Option<Vector3d>
  control: i8
  method_1323: Option<Vector3d>
  orientation: Option<Vector3d>
  position: Option<Vector3d>
});

struct_impl!(LocaleStruct {
  images: Vec<ImagePair>
  strings: Vec<StringPair>
});

struct_impl!(ImagePair {
  key: Option<String>
  value: Vec<u8>
});

struct_impl!(GarageItemInfo {
  category: ItemCategory
  item_view_category: ItemViewCategory
  modification_index: i32
  mounted: bool
  name: Option<String>
  position: i32
  premium_item: bool
  preview: ResourceId
  remaing_time_in_ms: i32
});

struct_impl!(DominationSounds {
  method_744: ResourceId
  method_491: ResourceId
  method_2387: ResourceId
  method_2280: ResourceId
  method_793: ResourceId
  method_656: ResourceId
  method_402: ResourceId
  method_95: ResourceId
  method_383: ResourceId
  method_1969: ResourceId
});

struct_impl!(DominationResources {
  method_1141: ResourceId
  method_2672: ResourceId
  method_2738: ResourceId
  method_2138: ResourceId
  method_1045: ResourceId
  method_2096: ResourceId
  method_2753: ResourceId
  name_65: ResourceId
  method_2099: ResourceId
  method_1098: ResourceId
  method_925: ResourceId
  method_547: ResourceId
});

struct_impl!(DailyQuestPrizeInfo {
  count: i32
  name: Option<String>
});

struct_impl!(DailyQuestInfo {
  method_2718: bool
  description: Option<String>
  method_2630: i32
  image: ResourceId
  method_562: Vec<DailyQuestPrizeInfo>
  progress: i32
  method_2366: i32
});

struct_impl!(ControlPointsCC {
  method_337: f32
  name_47: f32
  method_1123: f32
  name_43: Vec<ClientPointData>
  resources: DominationResources
  name_8: DominationSounds
});

struct_impl!(ClientPointData {
  id: i32
  name: Option<String>
  position: Option<Vector3d>
  score: f32
  method_2190: f32
  state: ControlPointState
  method_2697: Option<Vec<Option<String>>>
});

struct_impl!(ClientFlag {
  method_1384: Option<Vector3d>
  method_1275: Option<String>
  name_81: Option<Vector3d>
});

struct_impl!(ClientAssaultFlag {
  method_1384: Option<Vector3d>
  method_1275: Option<String>
  name_81: Option<Vector3d>
  id: i32
});

struct_impl!(ChatMessage {
  source_user_status: Option<UserStatus>
  system: bool
  target_user_status: Option<UserStatus>
  text: Option<String>
  warning: bool
});

struct_impl!(ChatCC {
  admin: bool
  antiflood_enabled: bool
  buffer_size: i32
  chat_enabled: bool
  chat_moderator_level: ChatModeratorLevel
  links_white_list: Option<Vec<Option<String>>>
  min_char: i32
  min_word: i32
  self_name: Option<String>
  show_links: bool
  typing_speed_antiflood_enabled: bool
});

struct_impl!(CaptureTheFlagSoundFX {
  name_55: ResourceId
  name_79: ResourceId
  name_37: ResourceId
  name_71: ResourceId
});

struct_impl!(CaptureTheFlagCC {
  method_2047: ClientFlag
  method_1345: ResourceId
  method_1814: ResourceId
  method_1229: ClientFlag
  method_1505: ResourceId
  method_872: ResourceId
  name_8: CaptureTheFlagSoundFX
});

struct_impl!(BonusInfoCC {
  bottom_text: Option<String>
  image: ResourceId
  top_text: Option<String>
});

struct_impl!(BattleNotifierData {
  battle_data: BattleInfoData
  user_id: Option<String>
});

struct_impl!(BattleMineCC {
  method_1937: ResourceId
  method_1406: i32
  method_2407: Vec<BattleMine>
  method_2285: ResourceId
  method_2634: ResourceId
  method_2393: ResourceId
  explosion_mark_texture: ResourceId
  explosion_sound: ResourceId
  method_2024: f32
  method_2226: ResourceId
  method_1764: ResourceId
  impact_force: f32
  method_2618: ResourceId
  name_45: f32
  method_2145: ResourceId
  method_472: f32
  radius: f32
  method_1957: ResourceId
});

struct_impl!(BattleMine {
  mine_id: Option<String>
  owner_id: Option<String>
  position: Option<Vector3d>
});

struct_impl!(BattleLimits {
  score_limit: i32
  time_limit_in_sec: i32
});

struct_impl!(BattleInviteMessage {
  available_rank: bool
  available_slot: bool
  battle_id: Option<String>
  map_name: Option<String>
  mode: BattleMode
  no_supplies_battle: bool
  private_battle: bool
});

struct_impl!(BattleInviteCC {
  method_321: ResourceId
});

struct_impl!(BattleInfoUser {
  kills: i32
  score: i32
  suspicious: bool
  user: Option<String>
});

struct_impl!(BattleInfoData {
  battle_id: Option<String>
  map_name: Option<String>
  mode: BattleMode
  private_battle: bool
  pro_battle: bool
  range: Range
  server_number: i32
});

struct_impl!(BattleCreateParameters {
  auto_balance: bool
  battle_mode: BattleMode
  equipment_constraints_mode: EquipmentConstraintsMode
  friendly_fire: bool
  method_1309: BattleLimits
  map_id: Option<String>
  max_people_count: i32
  name: Option<String>
  parkour_mode: bool
  private_battle: bool
  pro_battle: bool
  rank_range: Range
  re_armor_enabled: bool
  theme: MapTheme
  without_bonuses: bool
  without_crystals: bool
  without_supplies: bool
});

struct_impl!(AssaultSoundFX {
  name_55: ResourceId
  name_79: ResourceId
  name_37: ResourceId
  name_71: ResourceId
});

struct_impl!(AssaultCC {
  method_874: Vec<ClientAssaultFlag>
  method_2535: ResourceId
  method_1333: ResourceId
  method_1036: ResourceId
  method_2134: ResourceId
  method_993: Vec<AssaultBase>
  name_8: AssaultSoundFX
});

struct_impl!(AssaultBase {
  id: i32
  position: Option<Vector3d>
});

struct_impl!(AchievementCC {
  method_2426: Vec<Achievement>
});
