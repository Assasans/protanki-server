package jp.assasans.protanki.server.commands

enum class CommandName(val category: CommandCategory, val key: String, val side: CommandSide) {
  GetAesData(CommandCategory.System, "get_aes_data", CommandSide.Server),
  SetAesData(CommandCategory.System, "set_aes_data", CommandSide.Client),

  InitLocale(CommandCategory.System, "init_locale", CommandSide.Client),

  LoadResources(CommandCategory.System, "load_resources", CommandSide.Client),
  MainResourcesLoaded(CommandCategory.System, "main_resources_loaded", CommandSide.Client),
  DependenciesLoaded(CommandCategory.System, "dependencies_loaded", CommandSide.Server),

  Auth(CommandCategory.Auth, "auth", CommandSide.Server),
  AuthAccept(CommandCategory.Auth, "accept", CommandSide.Client),
  AuthDenied(CommandCategory.Auth, "denied", CommandSide.Client),
  InitExternalModel(CommandCategory.Auth, "init_external_model", CommandSide.Client),
  InitRegistrationModel(CommandCategory.Auth, "init_registration_model", CommandSide.Client),

  LoginByHash(CommandCategory.Auth, "login_by_hash", CommandSide.Server),
  LoginByHashFailed(CommandCategory.Auth, "login_by_hash_failed", CommandSide.Client),

  Error(CommandCategory.Lobby, "error", CommandSide.Server),

  InitPremium(CommandCategory.Lobby, "init_premium", CommandSide.Client),
  InitPanel(CommandCategory.Lobby, "init_panel", CommandSide.Client),
  ShowAchievements(CommandCategory.Lobby, "show_achievements", CommandSide.Client),

  InitFriendsList(CommandCategory.Lobby, "init_friends_list", CommandSide.Client),
  ShowFriendsList(CommandCategory.Lobby, "showFriends", CommandSide.Server),
  FriendsModalLoaded(CommandCategory.Lobby, "friends_onUsersLoaded", CommandSide.Client),

  InitBattleCreate(CommandCategory.Lobby, "init_battle_create", CommandSide.Client),
  InitBattleSelect(CommandCategory.Lobby, "init_battle_select", CommandSide.Client),

  InitMessages(CommandCategory.LobbyChat, "init_messages", CommandSide.Client),
  UnloadChat(CommandCategory.Lobby, "unload_chat", CommandSide.Client),

  ChangeLayout(CommandCategory.Lobby, "change_layout_state", CommandSide.Client),

  SubscribeUserUpdate(CommandCategory.Lobby, "subscribe_user_update", CommandSide.Server),

  update_rang_progress(CommandCategory.Lobby, "update_rang_progress", CommandSide.Client),

  BattleSelect(CommandCategory.BattleSelect, "select", CommandSide.Server),
  Fight(CommandCategory.BattleSelect, "fight", CommandSide.Server),
  UnloadBattleSelect(CommandCategory.Lobby, "unload_battle_select", CommandSide.Client),
  ShowBattleInfo(CommandCategory.Lobby, "show_battle_info", CommandSide.Client),

  StartBattle(CommandCategory.Lobby, "start_battle", CommandSide.Client),

  InitBonusesData(CommandCategory.Battle, "init_bonuses_data", CommandSide.Client),
  InitBonuses(CommandCategory.Battle, "init_bonuses", CommandSide.Client),

  InitShotsData(CommandCategory.Battle, "init_shots_data", CommandSide.Client),
  InitBattleModel(CommandCategory.Battle, "init_battle_model", CommandSide.Client),
  InitSuicideModel(CommandCategory.Battle, "init_suicide_model", CommandSide.Client),
  InitGuiModel(CommandCategory.Battle, "init_gui_model", CommandSide.Client),
  InitInventory(CommandCategory.Battle, "init_inventory", CommandSide.Client),
  InitMineModel(CommandCategory.Battle, "init_mine_model", CommandSide.Client),
  InitEffects(CommandCategory.Battle, "init_effects", CommandSide.Client),

  InitTank(CommandCategory.Battle, "init_tank", CommandSide.Client),
  GetInitDataLocalTank(CommandCategory.Battle, "get_init_data_local_tank", CommandSide.Server),

  PrepareToSpawn(CommandCategory.Battle, "prepare_to_spawn", CommandSide.Client),
  ChangeHealth(CommandCategory.Battle, "change_health", CommandSide.Client),
  SpawnTank(CommandCategory.Battle, "spawn", CommandSide.Client),
  ActivateTank(CommandCategory.Battle, "activate_tank", CommandSide.Client),

  InitStatisticsModel(CommandCategory.Battle, "init_statistics_model", CommandSide.Client),
  InitDmStatistics(CommandCategory.Battle, "init_dm_statistics", CommandSide.Client),
  UpdatePlayerStatistics(CommandCategory.Battle, "update_player_statistic", CommandSide.Client),

  Ping(CommandCategory.Battle, "ping", CommandSide.Server),
  Pong(CommandCategory.Battle, "pong", CommandSide.Client),

  DisablePause(CommandCategory.Battle, "disablePause", CommandSide.Server),

  Move(CommandCategory.Battle, "move", CommandSide.Server),
  FullMove(CommandCategory.Battle, "fullMove", CommandSide.Server),

  RotateTurret(CommandCategory.Battle, "rotateTurret", CommandSide.Server),

  ;

  companion object {
    private val map = CommandName.values().associateBy(CommandName::key)

    fun get(key: String) = map[key]
  }
}
