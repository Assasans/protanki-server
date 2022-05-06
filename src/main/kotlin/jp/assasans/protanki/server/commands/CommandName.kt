package jp.assasans.protanki.server.commands

enum class CommandName(val category: CommandCategory, val key: String, val side: CommandSide) {
  GetAesData(CommandCategory.System, "get_aes_data", CommandSide.Server),
  SetAesData(CommandCategory.System, "set_aes_data", CommandSide.Client),

  InitLocale(CommandCategory.System, "init_locale", CommandSide.Client),
  InitRegistrationModel(CommandCategory.System, "init_registration_model", CommandSide.Client),

  LoadResources(CommandCategory.System, "load_resources", CommandSide.Client),
  MainResourcesLoaded(CommandCategory.System, "main_resources_loaded", CommandSide.Client),
  DependenciesLoaded(CommandCategory.System, "dependencies_loaded", CommandSide.Server),

  Login(CommandCategory.Auth, "login", CommandSide.Server),
  AuthAccept(CommandCategory.Auth, "accept", CommandSide.Client),
  AuthDenied(CommandCategory.Auth, "denied", CommandSide.Client),
  InitExternalModel(CommandCategory.Auth, "init_external_model", CommandSide.Client),

  LoginByHash(CommandCategory.Auth, "loginByHash", CommandSide.Server),
  LoginByHashFailed(CommandCategory.Auth, "login_by_hash_failed", CommandSide.Client),

  SwitchToRegistration(CommandCategory.Registration, "set_state", CommandSide.Server),

  RefreshLobbyCaptcha(CommandCategory.Lobby, "refresh_captcha", CommandSide.Server),
  RefreshRegistrationCaptcha(CommandCategory.Registration, "refresh_captcha", CommandSide.Server),

  UpdateCaptcha(CommandCategory.Auth, "update_captcha", CommandSide.Client),

  Error(CommandCategory.Lobby, "error", CommandSide.Server),

  InitPremium(CommandCategory.Lobby, "init_premium", CommandSide.Client),
  InitPanel(CommandCategory.Lobby, "init_panel", CommandSide.Client),
  ShowAchievements(CommandCategory.Lobby, "show_achievements", CommandSide.Client),

  InitFriendsList(CommandCategory.Lobby, "init_friends_list", CommandSide.Client),
  ShowFriendsList(CommandCategory.Lobby, "showFriends", CommandSide.Server),
  ClientShowFriendsList(CommandCategory.Lobby, "friends_onUsersLoaded", CommandSide.Client),
  CheckFriendUsername(CommandCategory.Lobby, "friend_check", CommandSide.Server),
  FriendUsernameExists(CommandCategory.Lobby, "friend_check_exist", CommandSide.Client),
  FriendUsernameNotExists(CommandCategory.Lobby, "friend_check_not_exist", CommandSide.Client),
  FriendUsernameAlreadyAccepted(CommandCategory.Lobby, "friend_alreadyInAcceptedFriends", CommandSide.Client),
  FriendUsernameAlreadyIncoming(CommandCategory.Lobby, "friend_alreadyInIncomingFriends", CommandSide.Client),
  FriendUsernameAlreadyOutgoing(CommandCategory.Lobby, "friend_alreadyInOutgoingFriends", CommandSide.Client),
  AcceptFriendRequest(CommandCategory.Lobby, "friend_accept", CommandSide.Server),
  SendFriendRequest(CommandCategory.Lobby, "friend_add", CommandSide.Server),
  FriendAddAccepted(CommandCategory.Lobby, "friend_addToAccepted", CommandSide.Client),
  FriendAddIncoming(CommandCategory.Lobby, "friend_addToIncoming", CommandSide.Client),
  FriendAddOutgoing(CommandCategory.Lobby, "friend_addToOutgoing", CommandSide.Client),
  FriendRemoveAccepted(CommandCategory.Lobby, "friend_removeFromAccepted", CommandSide.Client),
  FriendRemoveIncoming(CommandCategory.Lobby, "friend_removeFromIncoming", CommandSide.Client),
  FriendRemoveOutgoing(CommandCategory.Lobby, "friend_removeFromOutgoing", CommandSide.Client),
  FriendRemoveNewAccepted(CommandCategory.Lobby, "remove_new_accepted_friend", CommandSide.Client),
  ClientFriendRemoveNewAccepted(CommandCategory.Lobby, "remove_new_accepted_friend", CommandSide.Client),
  FriendRemoveNewIncoming(CommandCategory.Lobby, "remove_new_incoming_friend", CommandSide.Client),
  ClientFriendRemoveNewIncoming(CommandCategory.Lobby, "remove_new_incoming_friend", CommandSide.Client),
  FriendRemove(CommandCategory.Lobby, "friend_breakItOff", CommandSide.Server),
  FriendReject(CommandCategory.Lobby, "friend_reject", CommandSide.Server),
  FriendRejectAll(CommandCategory.Lobby, "friend_rejectAllIncoming", CommandSide.Server),

  ShowSettings(CommandCategory.Lobby, "showSettings", CommandSide.Server),
  ClientShowSettings(CommandCategory.Lobby, "showSettings", CommandSide.Client),
  CheckPasswordIsSet(CommandCategory.Lobby, "checkPasswordIsSet", CommandSide.Server),
  PasswordIsSet(CommandCategory.Lobby, "notifyPasswordIsSet", CommandSide.Client),
  PasswordIsNotSet(CommandCategory.Lobby, "notifyPasswordIsNotSet", CommandSide.Client),

  InitBattleCreate(CommandCategory.Lobby, "init_battle_create", CommandSide.Client),
  InitBattleSelect(CommandCategory.Lobby, "init_battle_select", CommandSide.Client),

  InitMessages(CommandCategory.LobbyChat, "init_messages", CommandSide.Client),
  UnloadChat(CommandCategory.Lobby, "unload_chat", CommandSide.Client),
  SendChatMessageServer(CommandCategory.Lobby, "chat_message", CommandSide.Server),
  SendChatMessageClient(CommandCategory.LobbyChat, "__SYNTHETIC__", CommandSide.Client),

  SendBattleChatMessageServer(CommandCategory.Battle, "chat", CommandSide.Server),
  SendBattleChatMessageClient(CommandCategory.Battle, "chat", CommandSide.Client),
  SendBattleChatSpectatorMessageClient(CommandCategory.Battle, "spectator_message", CommandSide.Client),
  SendBattleChatSpectatorTeamMessageClient(CommandCategory.Battle, "spectator_team_message", CommandSide.Client),

  StartLayoutSwitch(CommandCategory.Lobby, "change_layout_state", CommandSide.Client),
  EndLayoutSwitch(CommandCategory.Lobby, "end_layout_switch", CommandSide.Client),

  SwitchBattleSelect(CommandCategory.Lobby, "switch_battle_select", CommandSide.Server),
  SwitchGarage(CommandCategory.Lobby, "switch_garage", CommandSide.Server),
  UnloadGarage(CommandCategory.Lobby, "unload_garage", CommandSide.Client),

  UnloadBattle(CommandCategory.Battle, "unload_battle", CommandSide.Client),

  ExitFromBattle(CommandCategory.Lobby, "exitFromBattle", CommandSide.Server),
  BattlePlayerRemove(CommandCategory.Battle, "remove_user", CommandSide.Client),

  BattlePlayerJoinDm(CommandCategory.Battle, "user_connect_dm", CommandSide.Client),
  BattlePlayerLeaveDm(CommandCategory.Battle, "user_disconnect_dm", CommandSide.Client),

  BattlePlayerJoinTeam(CommandCategory.Battle, "user_connect_team", CommandSide.Client),
  BattlePlayerLeaveTeam(CommandCategory.Battle, "user_disconnect_team", CommandSide.Client),

  SubscribeUserUpdate(CommandCategory.Lobby, "subscribe_user_update", CommandSide.Server),

  ShowDamageEnabled(CommandCategory.Lobby, "showDamageEnabled", CommandSide.Server),
  UpdateRankProgress(CommandCategory.Lobby, "update_rang_progress", CommandSide.Client),

  AddBattle(CommandCategory.Lobby, "add_battle", CommandSide.Client),
  CreateBattle(CommandCategory.BattleCreate, "battle_create", CommandSide.Server),
  CheckBattleName(CommandCategory.BattleCreate, "checkBattleNameForForbiddenWords", CommandSide.Server),
  SetCreateBattleName(CommandCategory.Lobby, "setFilteredBattleName", CommandSide.Client),

  SelectBattle(CommandCategory.BattleSelect, "select", CommandSide.Server),
  ClientSelectBattle(CommandCategory.BattleSelect, "select", CommandSide.Client),
  Fight(CommandCategory.BattleSelect, "fight", CommandSide.Server),
  UnloadBattleSelect(CommandCategory.Lobby, "unload_battle_select", CommandSide.Client),
  ShowBattleInfo(CommandCategory.Lobby, "show_battle_info", CommandSide.Client),

  JoinAsSpectator(CommandCategory.BattleSelect, "joinAsSpectator", CommandSide.Server),
  InitSpectatorUser(CommandCategory.Battle, "spectator_user_init", CommandSide.Server),
  UpdateSpectatorsList(CommandCategory.Battle, "update_spectator_list", CommandSide.Client),

  StartBattle(CommandCategory.Lobby, "start_battle", CommandSide.Client),

  InitBonusesData(CommandCategory.Battle, "init_bonuses_data", CommandSide.Client),
  InitBonuses(CommandCategory.Battle, "init_bonuses", CommandSide.Client),

  InitShotsData(CommandCategory.Battle, "init_shots_data", CommandSide.Client),
  InitBattleModel(CommandCategory.Battle, "init_battle_model", CommandSide.Client),
  InitSuicideModel(CommandCategory.Battle, "init_suicide_model", CommandSide.Client),
  InitDmModel(CommandCategory.Battle, "init_dm_model", CommandSide.Client),
  InitTdmModel(CommandCategory.Battle, "init_tdm_model", CommandSide.Client),
  InitCtfModel(CommandCategory.Battle, "init_ctf_model", CommandSide.Client),
  InitDomModel(CommandCategory.Battle, "init_dom_model", CommandSide.Client),
  InitGuiModel(CommandCategory.Battle, "init_gui_model", CommandSide.Client),
  InitMineModel(CommandCategory.Battle, "init_mine_model", CommandSide.Client),
  InitEffects(CommandCategory.Battle, "init_effects", CommandSide.Client),
  InitFlags(CommandCategory.Battle, "init_flags", CommandSide.Client),

  ChangeFund(CommandCategory.Battle, "change_fund", CommandSide.Client),
  ChangeTeamScore(CommandCategory.Battle, "change_team_scores", CommandSide.Client),

  InitTank(CommandCategory.Battle, "init_tank", CommandSide.Client),
  GetInitDataLocalTank(CommandCategory.Battle, "get_init_data_local_tank", CommandSide.Server),

  /**
   * The camera ready to move to the spawn point
   */
  ReadyToRespawn(CommandCategory.Battle, "readyToSpawn", CommandSide.Server),

  /**
   * The camera moved to the spawn point
   */
  ReadyToSpawn(CommandCategory.Battle, "readyToPlace", CommandSide.Server),
  PrepareToSpawn(CommandCategory.Battle, "prepare_to_spawn", CommandSide.Client),
  ChangeHealth(CommandCategory.Battle, "change_health", CommandSide.Client),
  SpawnTank(CommandCategory.Battle, "spawn", CommandSide.Client),
  ActivateTank(CommandCategory.Battle, "activate_tank", CommandSide.Client),
  ChangeTankSpecification(CommandCategory.Battle, "change_spec_tank", CommandSide.Client),

  InitInventory(CommandCategory.Battle, "init_inventory", CommandSide.Client),
  ActivateItem(CommandCategory.Battle, "activate_item", CommandSide.Server),
  ClientActivateItem(CommandCategory.Battle, "activate_item", CommandSide.Client),

  SelfDestruct(CommandCategory.Battle, "suicide", CommandSide.Server),

  InitStatisticsModel(CommandCategory.Battle, "init_statistics_model", CommandSide.Client),
  InitDmStatistics(CommandCategory.Battle, "init_dm_statistics", CommandSide.Client),
  InitTeamStatistics(CommandCategory.Battle, "init_team_statistics", CommandSide.Client),
  UpdatePlayerStatistics(CommandCategory.Battle, "update_player_statistic", CommandSide.Client),

  Ping(CommandCategory.Battle, "ping", CommandSide.Server),
  Pong(CommandCategory.Battle, "pong", CommandSide.Client),

  DisablePause(CommandCategory.Battle, "disablePause", CommandSide.Server),

  Move(CommandCategory.Battle, "move", CommandSide.Server),
  FullMove(CommandCategory.Battle, "fullMove", CommandSide.Server),
  RotateTurret(CommandCategory.Battle, "rotateTurret", CommandSide.Server),
  MovementControl(CommandCategory.Battle, "movementControl", CommandSide.Server),

  ClientMove(CommandCategory.Battle, "move", CommandSide.Client),
  ClientFullMove(CommandCategory.Battle, "fullMove", CommandSide.Client),
  ClientRotateTurret(CommandCategory.Battle, "rotateTurret", CommandSide.Client),
  ClientMovementControl(CommandCategory.Battle, "movementControl", CommandSide.Client),

  KillTank(CommandCategory.Battle, "kill_tank", CommandSide.Client),
  KillLocalTank(CommandCategory.Battle, "local_user_killed", CommandSide.Client),

  EnableEffect(CommandCategory.Battle, "enable_effect", CommandSide.Client),
  DisableEffect(CommandCategory.Battle, "disable_effect", CommandSide.Client),

  AddMine(CommandCategory.Battle, "put_mine", CommandSide.Client),
  ActivateMine(CommandCategory.Battle, "activate_mine", CommandSide.Client),
  RemoveMines(CommandCategory.Battle, "remove_mines", CommandSide.Client),

  StartFire(CommandCategory.Battle, "start_fire", CommandSide.Server),
  StopFire(CommandCategory.Battle, "stop_fire", CommandSide.Server),

  ClientStartFire(CommandCategory.Battle, "start_fire", CommandSide.Client),
  ClientStopFire(CommandCategory.Battle, "stop_fire", CommandSide.Client),

  Fire(CommandCategory.Battle, "fire", CommandSide.Server),
  FireDummy(CommandCategory.Battle, "fire_dummy", CommandSide.Server),
  FireStatic(CommandCategory.Battle, "fire_static", CommandSide.Server),
  FireTarget(CommandCategory.Battle, "fire_target", CommandSide.Server),

  Shot(CommandCategory.Battle, "shot", CommandSide.Client),
  ShotStatic(CommandCategory.Battle, "static_shot", CommandSide.Client),
  ShotTarget(CommandCategory.Battle, "target_shot", CommandSide.Client),

  SetTarget(CommandCategory.Battle, "set_target", CommandSide.Server),
  ResetTarget(CommandCategory.Battle, "reset_target", CommandSide.Server),

  ClientSetTarget(CommandCategory.Battle, "set_target", CommandSide.Client),
  ClientResetTarget(CommandCategory.Battle, "reset_target", CommandSide.Client),

  DamageTank(CommandCategory.Battle, "damage_tank", CommandSide.Client),

  SpawnBonus(CommandCategory.Battle, "spawn_bonus", CommandSide.Client),
  TryActivateBonus(CommandCategory.Battle, "attempt_to_take_bonus", CommandSide.Server),
  ActivateBonus(CommandCategory.Battle, "bonus_taken", CommandSide.Client),
  RemoveBonus(CommandCategory.Battle, "remove_bonus", CommandSide.Client),

  SpawnGold(CommandCategory.Battle, "gold_spawn", CommandSide.Client),

  // Used for taking and delivering flags
  TriggerFlag(CommandCategory.Battle, "attempt_to_take_flag", CommandSide.Server),
  DropFlag(CommandCategory.Battle, "flag_drop", CommandSide.Server),

  FlagCaptured(CommandCategory.Battle, "flagTaken", CommandSide.Client),
  FlagDropped(CommandCategory.Battle, "flag_drop", CommandSide.Client),
  FlagDelivered(CommandCategory.Battle, "deliver_flag", CommandSide.Client),
  FlagReturned(CommandCategory.Battle, "return_flag", CommandSide.Client),

  SetCrystals(CommandCategory.Lobby, "add_crystall", CommandSide.Client),
  SetScore(CommandCategory.Lobby, "add_score", CommandSide.Client),
  SetRank(CommandCategory.Lobby, "update_rang", CommandSide.Client),

  SetBattleRank(CommandCategory.Battle, "update_rang", CommandSide.Client),

  InitGarageItems(CommandCategory.Garage, "init_garage_items", CommandSide.Client),
  InitMountedItem(CommandCategory.Garage, "init_mounted_item", CommandSide.Client),
  InitGarageMarket(CommandCategory.Garage, "init_market", CommandSide.Client),
  TryBuyItem(CommandCategory.Garage, "try_buy_item", CommandSide.Server),
  BuyItem(CommandCategory.Garage, "buy_item", CommandSide.Client),
  TryBuyKit(CommandCategory.Garage, "kitBought", CommandSide.Server),
  TryMountItem(CommandCategory.Garage, "try_mount_item", CommandSide.Server),
  TryMountPreviewItem(CommandCategory.Garage, "fit", CommandSide.Server),
  MountItem(CommandCategory.Garage, "mount_item", CommandSide.Client),
  SelectGarageCategory(CommandCategory.Garage, "showCategory", CommandSide.Client),
  SelectGarageItem(CommandCategory.Garage, "select", CommandSide.Client),

  ;

  companion object {
    fun get(key: String, side: CommandSide) = values()
      .singleOrNull { command -> command.key == key && command.side == side }
  }

  override fun toString(): String = "${category.name}::$name"
}
