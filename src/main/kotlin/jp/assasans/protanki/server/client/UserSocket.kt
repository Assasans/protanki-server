package jp.assasans.protanki.server.client

import java.io.File
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mu.KotlinLogging
import jp.assasans.protanki.server.EncryptionTransformer
import jp.assasans.protanki.server.battles.BattlePlayer
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.CommandSide
import jp.assasans.protanki.server.exceptions.UnknownCommandCategoryException
import jp.assasans.protanki.server.exceptions.UnknownCommandException
import jp.assasans.protanki.server.readAvailable

suspend fun Command.send(socket: UserSocket) = socket.send(this)

class UserSocket(val socket: Socket) {
  private val logger = KotlinLogging.logger { }

  private val encryption = EncryptionTransformer()

  private val input: ByteReadChannel = socket.openReadChannel()
  private val output: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)

  private val lock: Semaphore = Semaphore(1)
  // private val sendQueue: Queue<Command> = LinkedList()

  private val json: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  val remoteAddress: NetworkAddress
    get() = socket.remoteAddress

  var user: User? = null
    private set

  suspend fun send(command: Command) {
    lock.withPermit {
      output.writeFully(command.serialize().toByteArray())

      if(command.name != CommandName.Pong) {
        if(command.name == CommandName.LoadResources) {
          logger.trace { "Sent command ${command.category}::${command.name} ${command.args.drop(1)}" }
        } else {
          logger.trace { "Sent command ${command.category}::${command.name} ${command.args}" }
        }
      }
    }
  }

  private val dependenciesChannel: Channel<Int> = Channel(16)
  private val loadedDependencies: MutableList<Int> = mutableListOf()

  private var battleInit = false

  private suspend fun awaitDependency(id: Int) {
    if(loadedDependencies.contains(id)) return

    while(true) {
      val loaded = dependenciesChannel.receive()
      loadedDependencies.add(loaded)

      if(loaded == id) break
    }
  }

  private suspend fun processPacket(packet: String) {
    try {
      // val end = packet.takeLast(Command.Delimiter.length)
      // if(end != Command.Delimiter) throw Exception("Invalid packet end: $end")

      // val decrypted = encryption.decrypt(packet.dropLast(Command.Delimiter.length))
      if(packet.isEmpty()) return

      // logger.debug { "PKT: $packet" }
      val decrypted = encryption.decrypt(packet)

      // logger.debug { "Decrypt: $packet -> $decrypted" }

      val command = Command()
      command.readFrom(decrypted.toByteArray())

      if(command.name != CommandName.Ping) {
        logger.trace { "Received command ${command.category}::${command.name} ${command.args}" }
      }

      if(command.side != CommandSide.Server) throw Exception("Unsupported command: ${command.category}::${command.name}")

      when(command.name) {
        CommandName.Auth                 -> {
          val data = json.adapter(AuthData::class.java).fromJson(command.args[0])!!

          logger.debug { "User login: [ Username = '${data.login}', Password = '${data.password}', Captcha = ${if(data.captcha.isEmpty()) "*none*" else "'${data.captcha}'"} ]" }

          user = User(id = 1, username = data.login, rank = UserRank.Major, score = 1337, crystals = 666666)

          send(Command(CommandName.AuthAccept))

          loadLobby()
        }

        CommandName.DependenciesLoaded   -> {
          val id = command.args[0].toInt()

          logger.debug { "Loaded dependency $id" }

          dependenciesChannel.send(id)
        }

        CommandName.LoginByHash          -> {
          send(Command(CommandName.LoginByHashFailed))
        }

        CommandName.BattleSelect         -> {
          send(
            Command(
              CommandName.ShowBattleInfo,
              mutableListOf(
                // "{\"battleMode\":\"DM\",\"itemId\":\"493202bf695cc88a\",\"scoreLimit\":10,\"timeLimitInSec\":0,\"preview\":618467,\"maxPeopleCount\":8,\"name\":\"For newbies\",\"proBattle\":false,\"minRank\":1,\"maxRank\":5,\"roundStarted\":true,\"spectator\":false,\"withoutBonuses\":false,\"withoutCrystals\":false,\"withoutSupplies\":false,\"proBattleEnterPrice\":150,\"timeLeftInSec\":-1643210127,\"userPaidNoSuppliesBattle\":false,\"proBattleTimeLeftInSec\":-1,\"users\":[{\"kills\":10,\"score\":100,\"suspicious\":false,\"user\":\"KoT-MaKc_2004\"},{\"kills\":2,\"score\":20,\"suspicious\":false,\"user\":\"PRESTY\"},{\"kills\":8,\"score\":80,\"suspicious\":false,\"user\":\"SF-SteFan27-BG\"},{\"kills\":7,\"score\":70,\"suspicious\":false,\"user\":\"Miro_18\"}]};;"
                json.adapter(ShowDmBattleInfoData::class.java).toJson(
                  ShowDmBattleInfoData(
                    itemId = "493202bf695cc88a",
                    battleMode = "DM",
                    scoreLimit = 300,
                    timeLimitInSec = 600,
                    timeLeftInSec = 212,
                    preview = 388954,
                    maxPeopleCount = 8,
                    name = "ProTanki Server",
                    minRank = 0,
                    maxRank = 16,
                    spectator = false,
                    withoutBonuses = false,
                    withoutCrystals = false,
                    withoutSupplies = false,
                    users = listOf(
                      BattleUser(user = "Luminate", kills = 666, score = 1337)
                    ),
                    score = 123
                  )
                )
              )
            )
          )
        }

        CommandName.Fight                -> {
          // TODO(Assasans): Shit
          val resourcesMap1Reader = File("D:/ProTankiServer/src/main/resources/resources/maps/sandbox-summer-1.json").bufferedReader()
          val resourcesMap1 = resourcesMap1Reader.use { it.readText() }

          // TODO(Assasans): Shit
          val resourcesMap2Reader = File("D:/ProTankiServer/src/main/resources/resources/maps/sandbox-summer-2.json").bufferedReader()
          val resourcesMap2 = resourcesMap2Reader.use { it.readText() }

          // TODO(Assasans): Shit
          val resourcesMap3Reader = File("D:/ProTankiServer/src/main/resources/resources/maps/sandbox-summer-3.json").bufferedReader()
          val resourcesMap3 = resourcesMap3Reader.use { it.readText() }

          // TODO(Assasans): Shit
          val shotsDataReader = File("D:/ProTankiServer/src/main/resources/resources/shots-data.json").bufferedReader()
          val shotsData = shotsDataReader.use { it.readText() }

          // BattlePlayer(socket, this, null)

          send(Command(CommandName.ChangeLayout, mutableListOf("BATTLE")))
          send(Command(CommandName.UnloadBattleSelect))
          send(Command(CommandName.StartBattle))
          send(Command(CommandName.UnloadChat))

          send(Command(CommandName.InitShotsData, mutableListOf(shotsData)))

          send(Command(CommandName.LoadResources, mutableListOf(resourcesMap1, "4")))
          awaitDependency(4)

          send(Command(CommandName.LoadResources, mutableListOf(resourcesMap2, "5")))
          awaitDependency(5)

          send(Command(CommandName.LoadResources, mutableListOf(resourcesMap3, "6")))
          awaitDependency(6)

          send(
            Command(
              CommandName.InitBonusesData,
              mutableListOf(
                // "{\"bonuses\":[{\"lighting\":{\"attenuationBegin\":100,\"attenuationEnd\":500,\"color\":6250335,\"intensity\":1,\"time\":0},\"id\":\"nitro\",\"resourceId\":170010,\"lifeTime\":30},{\"lighting\":{\"attenuationBegin\":100,\"attenuationEnd\":500,\"color\":9348154,\"intensity\":1,\"time\":0},\"id\":\"damage\",\"resourceId\":170011,\"lifeTime\":30},{\"lighting\":{\"attenuationBegin\":100,\"attenuationEnd\":500,\"color\":7185722,\"intensity\":1,\"time\":0},\"id\":\"armor\",\"resourceId\":170006,\"lifeTime\":30},{\"lighting\":{\"attenuationBegin\":100,\"attenuationEnd\":500,\"color\":14605789,\"intensity\":1,\"time\":0},\"id\":\"health\",\"resourceId\":170009,\"lifeTime\":30},{\"lighting\":{\"attenuationBegin\":100,\"attenuationEnd\":500,\"color\":8756459,\"intensity\":1,\"time\":0},\"id\":\"crystall\",\"resourceId\":170007,\"lifeTime\":900},{\"lighting\":{\"attenuationBegin\":100,\"attenuationEnd\":500,\"color\":15044128,\"intensity\":1,\"time\":0},\"id\":\"gold\",\"resourceId\":170008,\"lifeTime\":30000}],\"cordResource\":1000065,\"parachuteInnerResource\":170005,\"parachuteResource\":170004,\"pickupSoundResource\":269321};;"
                json.adapter(InitBonusesDataData::class.java).toJson(
                  InitBonusesDataData(
                    bonuses = listOf(
                      BonusData(
                        lighting = BonusLightingData(color = 6250335),
                        id = "nitro",
                        resourceId = 170010
                      ),
                      BonusData(
                        lighting = BonusLightingData(color = 7185722),
                        id = "armor",
                        resourceId = 170006
                      ),
                      BonusData(
                        lighting = BonusLightingData(color = 14605789),
                        id = "health",
                        resourceId = 170009
                      ),
                      BonusData(
                        lighting = BonusLightingData(color = 8756459),
                        id = "crystall",
                        resourceId = 170007
                      ),
                      BonusData(
                        lighting = BonusLightingData(color = 15044128),
                        id = "gold",
                        resourceId = 170008
                      )
                    )
                  )
                )
              )
            )
          )

          send(
            Command(
              CommandName.InitBattleModel,
              mutableListOf(
                // "{\"kick_period_ms\":125000,\"map_id\":\"map_sandbox\",\"mapId\":663288,\"invisible_time\":3500,\"spectator\":false,\"active\":true,\"dustParticle\":110001,\"battleId\":\"493202bf695cc88a\",\"minRank\":1,\"maxRank\":5,\"skybox\":\"{\\\"top\\\":45572,\\\"front\\\":57735,\\\"back\\\":268412,\\\"bottom\\\":31494,\\\"left\\\":927961,\\\"right\\\":987391}\",\"sound_id\":584396,\"map_graphic_data\":\"{\\\"mapId\\\":\\\"map_sandbox\\\",\\\"mapTheme\\\":\\\"SUMMER\\\",\\\"angleX\\\":-0.8500000238418579,\\\"angleZ\\\":2.5,\\\"lightColor\\\":13090219,\\\"shadowColor\\\":5530735,\\\"fogAlpha\\\":0.25,\\\"fogColor\\\":10543615,\\\"farLimit\\\":10000,\\\"nearLimit\\\":5000,\\\"gravity\\\":1000,\\\"skyboxRevolutionSpeed\\\":0,\\\"ssaoColor\\\":2045258,\\\"dustAlpha\\\":0.75,\\\"dustDensity\\\":0.15000000596046448,\\\"dustFarDistance\\\":7000,\\\"dustNearDistance\\\":5000,\\\"dustParticle\\\":\\\"summer\\\",\\\"dustSize\\\":200}\"}"
                json.adapter(InitBattleModelData::class.java).toJson(
                  InitBattleModelData(
                    map_id = "map_sandbox",
                    mapId = 663288,
                    battleId = "493202bf695cc88a",
                    skybox = "{\"top\":45572,\"front\":57735,\"back\":268412,\"bottom\":31494,\"left\":927961,\"right\":987391}",
                    map_graphic_data = "{\"mapId\":\"map_sandbox\",\"mapTheme\":\"SUMMER\",\"angleX\":-0.8500000238418579,\"angleZ\":2.5,\"lightColor\":13090219,\"shadowColor\":5530735,\"fogAlpha\":0.25,\"fogColor\":10543615,\"farLimit\":10000,\"nearLimit\":5000,\"gravity\":1000,\"skyboxRevolutionSpeed\":0,\"ssaoColor\":2045258,\"dustAlpha\":0.75,\"dustDensity\":0.15000000596046448,\"dustFarDistance\":7000,\"dustNearDistance\":5000,\"dustParticle\":\"summer\",\"dustSize\":200}"
                  )
                )
              )
            )
          )

          send(
            Command(
              CommandName.InitBonuses,
              mutableListOf(
                // "[]"
                json.adapter<List<InitBonusesData>>(
                  Types.newParameterizedType(List::class.java, InitBonusesData::class.java)
                ).toJson(listOf())
              )
            )
          )
        }

        CommandName.GetInitDataLocalTank -> {
          send(Command(CommandName.InitSuicideModel, mutableListOf("10000")))
          send(Command(CommandName.InitStatisticsModel, mutableListOf("For newbies")))

          send(
            Command(
              CommandName.InitGuiModel,
              mutableListOf(
                // "{\"name\":\"For newbies\",\"fund\":2.302999948596954,\"scoreLimit\":10,\"timeLimit\":0,\"currTime\":-1643210130,\"score_red\":0,\"score_blue\":0,\"team\":false,\"users\":[{\"nickname\":\"KoT-MaKc_2004\",\"rank\":5,\"teamType\":\"NONE\"},{\"nickname\":\"SF-SteFan27-BG\",\"rank\":4,\"teamType\":\"NONE\"},{\"nickname\":\"roflanebalo\",\"rank\":4,\"teamType\":\"NONE\"},{\"nickname\":\"Miro_18\",\"rank\":2,\"teamType\":\"NONE\"}]}"
                json.adapter(InitGuiModelData::class.java).toJson(
                  InitGuiModelData(
                    name = "ProTanki Server",
                    fund = 1337228,
                    scoreLimit = 300,
                    timeLimit = 600,
                    currTime = 212,
                    team = false,
                    users = listOf(
                      GuiUserData(nickname = "roflanebalo", rank = 4, teamType = "NONE"),
                      // GuiUserData(nickname = "Luminate", rank = 16, teamType = "NONE")
                    )
                  )
                )
              )
            )
          )
        }

        CommandName.SubscribeUserUpdate  -> {
          if(command.args[0] == "roflanebalo") {

          }
        }

        CommandName.Ping                 -> {
          if(!battleInit) {
            battleInit = true

            logger.info { "Init battle..." }

            send(
              Command(
                CommandName.InitDmStatistics,
                mutableListOf(
                  // "{\"users\":[{\"deaths\":0,\"kills\":0,\"score\":0,\"rank\":5,\"uid\":\"KoT-MaKc_2004\",\"chatModeratorLevel\":0},{\"deaths\":0,\"kills\":0,\"score\":0,\"rank\":4,\"uid\":\"SF-SteFan27-BG\",\"chatModeratorLevel\":0},{\"deaths\":0,\"kills\":0,\"score\":0,\"rank\":4,\"uid\":\"roflanebalo\",\"chatModeratorLevel\":0},{\"deaths\":0,\"kills\":0,\"score\":0,\"rank\":2,\"uid\":\"Miro_18\",\"chatModeratorLevel\":0}]}"
                  json.adapter(InitDmStatisticsData::class.java).toJson(
                    InitDmStatisticsData(
                      users = listOf(
                        DmStatisticsUserData(
                          uid = "roflanebalo",
                          rank = 4,
                          score = 666,
                          kills = 1000,
                          deaths = 7
                        ),
                        DmStatisticsUserData(
                          uid = "Luminate",
                          rank = 16,
                          score = 456,
                          kills = 777,
                          deaths = 333
                        )
                      )
                    )
                  )
                )
              )
            )

            send(
              Command(
                CommandName.InitInventory,
                mutableListOf(
                  // "{\"items\":[{\"id\":\"double_damage\",\"count\":26,\"slotId\":3,\"itemEffectTime\":55,\"itemRestSec\":20},{\"id\":\"armor\",\"count\":25,\"slotId\":2,\"itemEffectTime\":55,\"itemRestSec\":20},{\"id\":\"health\",\"count\":7,\"slotId\":1,\"itemEffectTime\":20,\"itemRestSec\":20},{\"id\":\"n2o\",\"count\":20,\"slotId\":4,\"itemEffectTime\":55,\"itemRestSec\":20}]}"
                  json.adapter(InitInventoryData::class.java).toJson(
                    InitInventoryData(
                      items = listOf(
                        InventoryItemData(
                          id = "health",
                          count = 1000,
                          slotId = 1,
                          itemEffectTime = 20,
                          itemRestSec = 20
                        ),
                        InventoryItemData(
                          id = "armor",
                          count = 1000,
                          slotId = 2,
                          itemEffectTime = 55,
                          itemRestSec = 20
                        ),
                        InventoryItemData(
                          id = "double_damage",
                          count = 1000,
                          slotId = 3,
                          itemEffectTime = 55,
                          itemRestSec = 20
                        ),
                        InventoryItemData(
                          id = "n2o",
                          count = 1000,
                          slotId = 4,
                          itemEffectTime = 55,
                          itemRestSec = 20
                        ),
                      )
                    )
                  )
                )
              )
            )

            send(
              Command(
                CommandName.InitMineModel,
                mutableListOf(
                  // "{\"activationTimeMsec\":1000,\"farVisibilityRadius\":10,\"nearVisibilityRadius\":7,\"impactForce\":3,\"minDistanceFromBase\":5,\"radius\":0.5,\"minDamage\":120,\"maxDamage\":240,\"resources\":{\"activateSound\":389057,\"deactivateSound\":965887,\"explosionSound\":175648,\"idleExplosionTexture\":545261,\"mainExplosionTexture\":965737,\"blueMineTexture\":925137,\"redMineTexture\":342637,\"enemyMineTexture\":975465,\"friendlyMineTexture\":523632,\"explosionMarkTexture\":962237,\"model3ds\":895671}}",
                  // "{\"mines\":[]}"
                  json.adapter(InitMineModelSettings::class.java).toJson(InitMineModelSettings()),
                  json.adapter(InitMineModelData::class.java).toJson(InitMineModelData())
                )
              )
            )

            send(
              Command(
                CommandName.InitTank,
                mutableListOf(
                  // "{\"battleId\":\"493202bf695cc88a\",\"colormap_id\":966681,\"hull_id\":\"hunter_m0\",\"turret_id\":\"railgun_m0\",\"team_type\":\"NONE\",\"partsObject\":\"{\\\"engineIdleSound\\\":386284,\\\"engineStartMovingSound\\\":226985,\\\"engineMovingSound\\\":75329,\\\"turretSound\\\":242699}\",\"hullResource\":227169,\"turretResource\":906685,\"sfxData\":\"{\\\"chargingPart1\\\":114424,\\\"chargingPart2\\\":468379,\\\"chargingPart3\\\":932241,\\\"hitMarkTexture\\\":670581,\\\"powTexture\\\":963502,\\\"ringsTexture\\\":966691,\\\"shotSound\\\":900596,\\\"smokeImage\\\":882103,\\\"sphereTexture\\\":212409,\\\"trailImage\\\":550305,\\\"lighting\\\":[{\\\"name\\\":\\\"charge\\\",\\\"light\\\":[{\\\"attenuationBegin\\\":200,\\\"attenuationEnd\\\":200,\\\"color\\\":5883129,\\\"intensity\\\":0.7,\\\"time\\\":0},{\\\"attenuationBegin\\\":200,\\\"attenuationEnd\\\":800,\\\"color\\\":5883129,\\\"intensity\\\":0.3,\\\"time\\\":600}]},{\\\"name\\\":\\\"shot\\\",\\\"light\\\":[{\\\"attenuationBegin\\\":100,\\\"attenuationEnd\\\":600,\\\"color\\\":5883129,\\\"intensity\\\":0.7,\\\"time\\\":0},{\\\"attenuationBegin\\\":1,\\\"attenuationEnd\\\":2,\\\"color\\\":5883129,\\\"intensity\\\":0,\\\"time\\\":300}]},{\\\"name\\\":\\\"hit\\\",\\\"light\\\":[{\\\"attenuationBegin\\\":200,\\\"attenuationEnd\\\":600,\\\"color\\\":5883129,\\\"intensity\\\":0.7,\\\"time\\\":0},{\\\"attenuationBegin\\\":1,\\\"attenuationEnd\\\":2,\\\"color\\\":5883129,\\\"intensity\\\":0,\\\"time\\\":300}]},{\\\"name\\\":\\\"rail\\\",\\\"light\\\":[{\\\"attenuationBegin\\\":100,\\\"attenuationEnd\\\":500,\\\"color\\\":5883129,\\\"intensity\\\":0.5,\\\"time\\\":0},{\\\"attenuationBegin\\\":1,\\\"attenuationEnd\\\":2,\\\"color\\\":5883129,\\\"intensity\\\":0,\\\"time\\\":1800}]}],\\\"bcsh\\\":[{\\\"brightness\\\":0,\\\"contrast\\\":0,\\\"saturation\\\":0,\\\"hue\\\":0,\\\"key\\\":\\\"trail\\\"},{\\\"brightness\\\":0,\\\"contrast\\\":0,\\\"saturation\\\":0,\\\"hue\\\":0,\\\"key\\\":\\\"charge\\\"}]}\",\"position\":\"0.0@0.0@0.0@0.0\",\"incration\":3268,\"tank_id\":\"roflanebalo\",\"nickname\":\"roflanebalo\",\"state\":\"suicide\",\"maxSpeed\":8,\"maxTurnSpeed\":1.3229597,\"acceleration\":9.09,\"reverseAcceleration\":11.74,\"sideAcceleration\":7.74,\"turnAcceleration\":2.2462387,\"reverseTurnAcceleration\":3.6576867,\"mass\":1761,\"power\":9.09,\"dampingCoeff\":1500,\"turret_turn_speed\":0.9815731713216109,\"health\":10000,\"rank\":4,\"kickback\":2.138,\"turretTurnAcceleration\":1.214225560612455,\"impact_force\":3.6958,\"state_null\":true}"
                  json.adapter(InitTankData::class.java).toJson(
                    InitTankData(
                      battleId = "493202bf695cc88a",
                      hull_id = "hunter_m0",
                      turret_id = "railgun_m0",
                      colormap_id = 966681,
                      hullResource = 227169,
                      turretResource = 906685,
                      partsObject = "{\"engineIdleSound\":386284,\"engineStartMovingSound\":226985,\"engineMovingSound\":75329,\"turretSound\":242699}",
                      tank_id = "roflanebalo",
                      nickname = "roflanebalo",
                      team_type = "NONE"
                    )
                  )
                )
              )
            )

            logger.info { "Load stage 2" }

            send(
              Command(
                CommandName.UpdatePlayerStatistics,
                mutableListOf(
                  // "{\"kills\":0,\"deaths\":0,\"id\":\"roflanebalo\",\"rank\":4,\"team_type\":\"NONE\",\"score\":0}"
                  json.adapter(UpdatePlayerStatisticsData::class.java).toJson(
                    UpdatePlayerStatisticsData(
                      id = "roflanebalo",
                      rank = 4,
                      team_type = "NONE",
                      score = 666,
                      kills = 1000,
                      deaths = 777
                    )
                  )
                )
              )
            )

            send(
              Command(
                CommandName.InitEffects,
                mutableListOf(json.adapter(InitEffectsData::class.java).toJson(InitEffectsData()))
              )
            )

            send(
              Command(
                CommandName.PrepareToSpawn,
                mutableListOf(
                  "roflanebalo",
                  "0.0@0.0@1000.0@0.0"
                )
              )
            )

            send(
              Command(
                CommandName.ChangeHealth,
                mutableListOf(
                  "roflanebalo",
                  "10000"
                )
              )
            )

            send(
              Command(
                CommandName.SpawnTank,
                mutableListOf(
                  json.adapter(SpawnTankData::class.java).toJson(
                    SpawnTankData(
                      tank_id = "roflanebalo",
                      health = 10000,
                      incration_id = 2,
                      team_type = "NONE",
                      x = 0.0,
                      y = 0.0,
                      z = 1000.0,
                      rot = 0.0
                    )
                  )
                )
              )
            )

            send(
              Command(
                CommandName.ActivateTank,
                mutableListOf(
                  "roflanebalo"
                )
              )
            )
          }

          send(Command(CommandName.Pong))
        }

        CommandName.Error                -> {
          val error = command.args[0]
          logger.error { "Client-side error occurred: $error" }
        }

        CommandName.ShowFriendsList      -> {
          send(
            Command(
              CommandName.ShowFriendsList,
              mutableListOf(json.adapter(ShowFriendsModalData::class.java).toJson(ShowFriendsModalData()))
            )
          )
        }

        else                             -> {}
      }
    } catch(exception: UnknownCommandCategoryException) {
      logger.warn { "Unknown command category: ${exception.category}" }
    } catch(exception: UnknownCommandException) {
      logger.warn { "Unknown command: ${exception.category}::${exception.command}" }
    } catch(exception: Exception) {
      logger.error(exception) { "An exception occurred" }
    }
  }

  suspend fun handle() {
    // awaitDependency can deadlock execution if suspended
    GlobalScope.launch { initClient() }

    try {
      while(!(input.isClosedForRead || input.isClosedForWrite)) {
        val buffer = input.readAvailable()
        val packets = String(buffer).split(Command.Delimiter)

        for(packet in packets) {
          // awaitDependency can deadlock execution if suspended
          GlobalScope.launch { processPacket(packet) }
        }
      }

      logger.debug { "${socket.remoteAddress} end of data" }
    } catch(exception: Throwable) {
      logger.error(exception) { "An exception occurred" }

      // withContext(Dispatchers.IO) {
      //   socket.close()
      // }
    }
  }

  private suspend fun loadLobby() {
    // TODO(Assasans): Shit
    val resourcesLobbyReader = File("D:/ProTankiServer/src/main/resources/resources/lobby.json").bufferedReader()
    val resourcesLobby = resourcesLobbyReader.use { it.readText() }

    // TODO(Assasans): Shit
    val mapsReader = File("D:/ProTankiServer/src/main/resources/maps.json").bufferedReader()
    val maps = mapsReader.use { it.readText() }

    send(
      Command(
        CommandName.InitPremium,
        mutableListOf(
          json.adapter(InitPremiumData::class.java).toJson(InitPremiumData())
        )
      )
    )

    send(
      Command(
        CommandName.InitPanel,
        mutableListOf(
          json.adapter(InitPanelData::class.java).toJson(InitPanelData())
        )
      )
    )

    send(
      Command(
        CommandName.update_rang_progress,
        mutableListOf("3668")
      )
    )

    send(
      Command(
        CommandName.InitFriendsList,
        mutableListOf(
          json.adapter(InitFriendsListData::class.java).toJson(
            InitFriendsListData(
              friends = listOf(
                // FriendEntry(id = "Luminate", rank = 16, online = false),
                // FriendEntry(id = "MoscowCity", rank = 18, online = true)
              )
            )
          )
        )
      )
    )

    send(Command(CommandName.ChangeLayout, mutableListOf("BATTLESELECT")))

    send(Command(CommandName.LoadResources, mutableListOf(resourcesLobby, "3")))
    awaitDependency(3)

    send(
      Command(
        CommandName.ShowAchievements,
        mutableListOf(
          json.adapter(ShowAchievementsData::class.java).toJson(ShowAchievementsData(ids = listOf(1, 3)))
        )
      )
    )

    send(
      Command(
        CommandName.InitMessages,
        mutableListOf(
          "{\"messages\":[{\"name\":\"1234raketa\",\"rang\":6,\"chatPermissions\":0,\"message\":\"+\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"есть такое)\",\"addressed\":true,\"chatPermissionsTo\":1,\"nameTo\":\"GVA\",\"rangTo\":15,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"1234raketa\",\"rang\":6,\"chatPermissions\":0,\"message\":\"странно привыкать к краскам с резитами)\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"я до уо1 чисто с релей играл)\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Red_Dragon\",\"rangTo\":7,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"S.Shadows-Brazil\",\"rang\":17,\"chatPermissions\":0,\"message\":\"#battle|Wasp and Fire|302969a56405e2b0 ro Wasp y Fire.\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"GVA\",\"rang\":15,\"chatPermissions\":1,\"message\":\"тут краски каченные еще до м4..\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"DougZ\",\"rang\":17,\"chatPermissions\":0,\"message\":\"#battle|Wasp and Fire|302969a56405e2b0 Entra ai wasp and fire\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"M_O_N_A_S_H_K_A\",\"rang\":7,\"chatPermissions\":0,\"message\":\"#battle|qartvelebo moit |ffcd251fcc756d6e\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"1234raketa\",\"rangTo\":6,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"1234raketa\",\"rang\":6,\"chatPermissions\":0,\"message\":\"#battle|qartvelebo moit |ffcd251fcc756d6e\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"https://www.youtube.com/watch?v=zzDf4tRUmfU\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"На канале zzeress проходит стрим! Заходи, будет интересно! https://youtu.be/QN2qwgn7YyA\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"koa noobs\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Waqtf\",\"rang\":8,\"chatPermissions\":0,\"message\":\"ктото хочет на полигон СР или на тишину СТF?\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"hahahahah boa mano\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"Qual é o nome da música\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"zhurv\",\"rang\":12,\"chatPermissions\":0,\"message\":\"#battle|Плато CTF|2979f072843c800a\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"musica do brasil\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"funk\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"eu entendo, mas queria o nome\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"\",\"rang\":0,\"chatPermissions\":0,\"message\":\"Большинство угонов аккаунтов происходит из-за слишком простых паролей. Задумайтесь над этим, сделайте пароль сложнее, чаще его меняйте! Защитите свой аккаунт от злоумышленников\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"sourceUserIp\":\"null\",\"targetUserIp\":\"null\",\"system\":true,\"yellow\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"da primeira\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"e uma mulher q canta ela\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"https://www.youtube.com/watch?v=zzDf4tRUmfU\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"ei\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"SOAD\",\"rang\":5,\"chatPermissions\":0,\"message\":\"#battle|Стрим WebSter и Zzeress|5480f4d421c4df0e\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"alguem sabe me dizer\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"S.Shadows-Brazil\",\"rang\":17,\"chatPermissions\":0,\"message\":\"#battle|Wasp y Fire brazil|8031c0ec28f272f7\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"yo\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Kratos\",\"rangTo\":5,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"se eu tivesse uma conta antigamente\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Waqtf\",\"rang\":8,\"chatPermissions\":0,\"message\":\"#battle|Тишина CTF|e85a3b80b4e6bae3\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"não, é um jogo independente do tanki\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Kratos\",\"rangTo\":5,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"eu posso logar nela nesse pro tanl?\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"На канале zzeress проходит стрим! Заходи, будет интересно! https://youtu.be/QN2qwgn7YyA\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"a tabom vlw\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"#battle|Стрим WebSter и Zzeress|5480f4d421c4df0e\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"#battle|Стрим WebSter и Zzeress|5480f4d421c4df0e\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"#battle|Стрим WebSter и Zzeress|5480f4d421c4df0e\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"alguma dica pra conseguir cristal\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"renatozikaa\",\"rang\":13,\"chatPermissions\":0,\"message\":\"é isso ai msm\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"o video tá fixe, deu sub\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"mano voce fala muito bem em brasileiro parabens\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"dei*\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"renatozikaa\",\"rang\":13,\"chatPermissions\":0,\"message\":\"vou sair tn\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"e upar?\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"ok ty\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"eu sou de Portugal, é fácil lol\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"angel tu e youtuber?\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"S.Shadows-Brazil\",\"rang\":17,\"chatPermissions\":0,\"message\":\"0 partidas. F\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"bem me parecia\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"graças a deus um tuga\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"Compra cristais\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Kratos\",\"rangTo\":5,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"Também és tuga?\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"claro\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"maravilha\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"fds, deste o real grind nesta merda\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"A_Viuva_Preta_Br\",\"rang\":10,\"chatPermissions\":0,\"message\":\"#battle|wasp and fire|7d1ad7e732b33147\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"Comprei muito e jogo desde que o jogo saiu\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"A_Viuva_Preta_Br\",\"rang\":10,\"chatPermissions\":0,\"message\":\"#battle|wasp and fire|7d1ad7e732b33147\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"ah ok\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"A_Viuva_Preta_Br\",\"rang\":10,\"chatPermissions\":0,\"message\":\"#battle|wasp and fire|7d1ad7e732b33147\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"Lisboeta?\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"do Porto ahahha\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"Portuense :)\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"Ah eu sou de Cascais\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"devo ir ao Porto no Verão\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"é bem mano, depois adiciona\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"Porto melhor cidade :v\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"hahahah estive no Porto em 2014\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"é bonito mas prefiro Lisboa\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"eu nunca fui a capital por acaso\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true}]}",
          "{\"antiFloodEnabled\":true,\"typingSpeedAntifloodEnabled\":true,\"bufferSize\":60,\"minChar\":60,\"minWord\":5,\"showLinks\":true,\"admin\":false,\"selfName\":\"roflanebalo\",\"chatModeratorLevel\":0,\"symbolCost\":176,\"enterCost\":880,\"chatEnabled\":true,\"linksWhiteList\":[\"h\",\"t\",\"t\",\"p\",\":\",\"/\",\"/\",\"g\",\"t\",\"a\",\"n\",\"k\",\"s\",\"-\",\"o\",\"n\",\"l\",\"i\",\"n\",\"e\",\".\",\"c\",\"o\",\"m\",\"/\",\"|\",\"h\",\"t\",\"t\",\"p\",\":\",\"/\",\"/\",\"v\",\"k\",\".\",\"c\",\"o\",\"m\",\"/\",\"e\",\"b\",\"a\",\"l\"]}"
          // json.adapter(InitChatMessagesData::class.java).toJson(
          //   InitChatMessagesData(
          //     messages = listOf(
          //       ChatMessage(name = "roflanebalo", rang = 4, message = "Ты пидорас")
          //     )
          //   )
          // ),
          // json.adapter(InitChatSettings::class.java).toJson(InitChatSettings())
        )
      )
    )

    val mapsParsed = json
      .adapter<List<Map>>(Types.newParameterizedType(List::class.java, Map::class.java))
      .fromJson(maps)!!

    send(
      Command(
        CommandName.InitBattleCreate,
        mutableListOf(
          json.adapter(InitBattleCreateData::class.java).toJson(
            InitBattleCreateData(
              battleLimits = listOf(
                BattleLimit(battleMode = "DM", scoreLimit = 999, timeLimitInSec = 59940),
                BattleLimit(battleMode = "TDM", scoreLimit = 999, timeLimitInSec = 59940),
                BattleLimit(battleMode = "CTF", scoreLimit = 999, timeLimitInSec = 59940),
                BattleLimit(battleMode = "CP", scoreLimit = 999, timeLimitInSec = 59940)
              ),
              maps = mapsParsed
            )
          )
        )
      )
    )

    send(
      Command(
        CommandName.InitBattleSelect,
        mutableListOf(
          // "{\"battles\":[{\"battleId\":\"da1a83bf9778a6ca\",\"battleMode\":\"DM\",\"map\":\"map_aleksandrovsk\",\"maxPeople\":24,\"name\":\"Александровск DM\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":4,\"maxRank\":10,\"preview\":388954,\"suspicious\":false,\"users\":[]},{\"battleId\":\"493202bf695cc88a\",\"battleMode\":\"DM\",\"map\":\"map_sandbox\",\"maxPeople\":8,\"name\":\"For newbies\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":1,\"maxRank\":5,\"preview\":618467,\"suspicious\":false,\"users\":[\"121\",\"Floki\",\"MrHensel\"]},{\"battleId\":\"7601c6d6333a8ef7\",\"battleMode\":\"CTF\",\"map\":\"map_serpuhov\",\"maxPeople\":10,\"name\":\"З А Л Е Т А Й ! ! !\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":1,\"maxRank\":7,\"preview\":303326,\"suspicious\":false,\"usersBlue\":[\"IchsCrewers\",\"urpok\",\"amiko\",\"WithoutPanic\",\"XaX\",\"N1RvAnA\",\"Tsulukaa\",\"HTML5\",\"Zed\",\"ASLA\"],\"usersRed\":[\"Votka_piva\",\"OverBro\",\"Tailand\",\"AK48\",\"Rakuzan\",\"Tryp\",\"kaikaci\",\"Don_Wripa\",\"Jife\",\"CHaKvee\"]},{\"battleId\":\"f5f72f6f39ba30a5\",\"battleMode\":\"CP\",\"map\":\"map_highland\",\"maxPeople\":5,\"name\":\"SERG JAN MTI\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":3,\"maxRank\":7,\"preview\":411251,\"suspicious\":false,\"usersBlue\":[\"CallMeHiemoBTW\"],\"usersRed\":[\"Anil1999\",\"riki_killer\"]},{\"battleId\":\"11b2c3e93bba26fa\",\"battleMode\":\"DM\",\"map\":\"map_sandbox\",\"maxPeople\":8,\"name\":\"For newbies\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":1,\"maxRank\":5,\"preview\":618467,\"suspicious\":false,\"users\":[\"Pitomec\",\"dimon_master\",\"Playerr\",\"Raillow\",\"Destrui_Z\"]},{\"battleId\":\"cb6795ecf3feb164\",\"battleMode\":\"CTF\",\"map\":\"map_highland\",\"maxPeople\":8,\"name\":\"Плато CTF\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":3,\"maxRank\":10,\"preview\":411251,\"suspicious\":false,\"usersBlue\":[\"Gold.Box.vil.de.drop\",\"CrySoul\",\"DIMONCHYC\",\"xachogoat\",\"Bayramov\",\"Naig\",\"vadiksid\",\"priboj22\"],\"usersRed\":[\"Crazy_Tankist725\",\"Shako\",\"dima666d\",\"JuzzerYT\",\"dorflexx9\",\"Borin\",\"Yurkens\",\"TheStrayRabdos\"]},{\"battleId\":\"705079a4b85a5855\",\"battleMode\":\"CTF\",\"map\":\"map_farm\",\"maxPeople\":4,\"name\":\"Фарм Криссталов!! ЗАХОДИ!\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":3,\"maxRank\":9,\"preview\":352418,\"suspicious\":false,\"usersBlue\":[\"OTELL\",\"Frone\",\"Damager\",\"KWATRO\"],\"usersRed\":[\"rekzyyll\",\"Dr.tornike\",\"TTuToH\",\"Godmode_XT\"]},{\"battleId\":\"2d32231912177a43\",\"battleMode\":\"CTF\",\"map\":\"map_rio\",\"maxPeople\":10,\"name\":\"Rio CTF\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":3,\"maxRank\":9,\"preview\":219191,\"suspicious\":false,\"usersBlue\":[\"temobere\",\"pepsi164\",\"Jopa_Bobra\",\"Ghassan909\",\"Cacique950\",\"MATKA\",\"oRtUdUsA\",\"IMeruliXapachuri\",\"BRADR\",\"Misha_1245\"],\"usersRed\":[\"FREMZ\",\"Monarquista_BR\",\"OGMikey\",\"Bacon\",\"Life_is_a_Dream\",\"Clothes\",\"ne_omlet\",\"izoro\",\"TRYAPOCHKA\",\"Ebrahim123fq\"]},{\"battleId\":\"fe12b5b47286b636\",\"battleMode\":\"TDM\",\"map\":\"map_farm\",\"maxPeople\":1,\"name\":\"Ферма TDM\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":3,\"maxRank\":10,\"preview\":352418,\"suspicious\":false,\"usersBlue\":[\"K.a.n.a.t.k.a\"],\"usersRed\":[\"Mr.KoFFe\"]},{\"battleId\":\"26b359588fe650c7\",\"battleMode\":\"CTF\",\"map\":\"map_highland\",\"maxPeople\":8,\"name\":\"Highland CTF\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":3,\"maxRank\":10,\"preview\":411251,\"suspicious\":false,\"usersBlue\":[\"Artak\",\"Gabo\",\"f4xxxie\",\"TemkaFRE\",\"dis1dexd\",\"Clownnnnn\",\"Ejyk_off\",\"A.L.I.E.N\"],\"usersRed\":[\"kristasek\",\"vintx\",\"Tribasc\",\"lll_TTo3uTuB_lll\",\"ELEGANT\",\"dtvoid1\",\"NagIBATOR_v_shleme\",\"Ajaxx\"]},{\"battleId\":\"eea9efb805a232b1\",\"battleMode\":\"DM\",\"map\":\"map_sandbox\",\"maxPeople\":8,\"name\":\"For newbies\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":1,\"maxRank\":5,\"preview\":618467,\"suspicious\":false,\"users\":[\"assilthekiller\",\"GiorgiiArabidzee\",\"Lashhaaa\",\"euphoria\"]},{\"battleId\":\"88905f83801d5829\",\"battleMode\":\"CTF\",\"map\":\"map_island\",\"maxPeople\":3,\"name\":\"Хж/Вж +50%\",\"privateBattle\":false,\"proBattle\":true,\"minRank\":9,\"maxRank\":17,\"preview\":538875,\"suspicious\":false,\"usersBlue\":[\"Ultimate\",\"Shelby_Tom\",\"beauty\"],\"usersRed\":[\"SeVeNaPcHuK\",\"Dep3ka9l_MaJlblLLlka\",\"MyJIbTuFpykT\"]},{\"battleId\":\"a8b35f6a7fcf45a7\",\"battleMode\":\"DM\",\"map\":\"map_sandbox\",\"maxPeople\":8,\"name\":\"Песочница DM\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":1,\"maxRank\":6,\"preview\":618467,\"suspicious\":false,\"users\":[\"L.E.G.E.N.D\",\"Markhosias\",\"Zhelezka_Toper\",\"KNOWME\",\"zeidoplay\",\"Solierka\",\"Cheetos\",\"Sosiska_228\"]},{\"battleId\":\"177e5773385851c7\",\"battleMode\":\"TDM\",\"map\":\"map_highland\",\"maxPeople\":8,\"name\":\"Плато TDM\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":3,\"maxRank\":10,\"preview\":411251,\"suspicious\":false,\"usersBlue\":[\"DoYouLikeBagdo\",\"shymsk\",\"Dedasheni\",\"HO4b\",\"Skvap_p\"],\"usersRed\":[\"FlossyX1337\",\"vlad432189\",\"mamashenismtyvneli\",\"fael\",\"28882_Russia\",\"ERMAK\"]},{\"battleId\":\"2e48dfc62de99231\",\"battleMode\":\"CTF\",\"map\":\"map_island\",\"maxPeople\":3,\"name\":\"ХЖ ВЖ 0% ccnem anadekvat.\",\"privateBattle\":false,\"proBattle\":true,\"minRank\":12,\"maxRank\":20,\"preview\":538875,\"suspicious\":false,\"usersBlue\":[\"WidaLoca\",\"DEATHMACHINE\",\"Prosto\"],\"usersRed\":[\"KTO\",\"YT_BoRsHiKBBG\",\"Gor\"]},{\"battleId\":\"5b98340297a4820b\",\"battleMode\":\"CTF\",\"map\":\"map_island\",\"maxPeople\":3,\"name\":\"ХЖ ВЖ\",\"privateBattle\":false,\"proBattle\":true,\"minRank\":13,\"maxRank\":21,\"preview\":538875,\"suspicious\":false,\"usersBlue\":[\"DVD\",\"S.Shadows-Brazil\",\"COJLEBAYA_COJLb\"],\"usersRed\":[\"Hesoyam\",\"Defaint\",\"Hounder\"]},{\"battleId\":\"6455e9bac769f5f3\",\"battleMode\":\"TDM\",\"map\":\"map_island\",\"maxPeople\":1,\"name\":\"Остров TDM\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":9,\"maxRank\":16,\"preview\":538875,\"suspicious\":false,\"usersBlue\":[],\"usersRed\":[]},{\"battleId\":\"494bed06e5654779\",\"battleMode\":\"CTF\",\"map\":\"map_highland\",\"maxPeople\":8,\"name\":\"Плато CTF\",\"privateBattle\":false,\"proBattle\":true,\"minRank\":7,\"maxRank\":15,\"preview\":411251,\"suspicious\":false,\"usersBlue\":[\"Avara\",\"zhurv\",\"am-am\",\"Waqtf\"],\"usersRed\":[\"Remanescente\",\"Fatch\",\"A_JI_K_A_H_A_B_T\",\"Den2017921\"]},{\"battleId\":\"a2cefd1630b6c41f\",\"battleMode\":\"CTF\",\"map\":\"map_boombox\",\"maxPeople\":4,\"name\":\"JOIN IF U  ARE NOT GAY\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":2,\"maxRank\":8,\"preview\":945441,\"suspicious\":false,\"usersBlue\":[\"Jigaro95\",\"xLukax\",\"GeoGamerRuso\",\"squiizzii\"],\"usersRed\":[\"Sleepy\",\"phenomen777\",\"nicky_ko_morry\",\"Luixx\"]},{\"battleId\":\"36bb28d75810f2ad\",\"battleMode\":\"CTF\",\"map\":\"map_skyscrapers\",\"maxPeople\":5,\"name\":\"Park\",\"privateBattle\":false,\"proBattle\":true,\"minRank\":9,\"maxRank\":16,\"preview\":56426,\"suspicious\":false,\"usersBlue\":[\"NoNamePlayer\"],\"usersRed\":[\"cl_l\",\"wthined\"]},{\"battleId\":\"256b84bcf8c7615b\",\"battleMode\":\"DM\",\"map\":\"map_madness_space\",\"maxPeople\":32,\"name\":\"Безумие DM\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":10,\"maxRank\":17,\"preview\":678652,\"suspicious\":false,\"users\":[\"Federator\",\"kl_nazariy\",\"Inflammable_Dragon\",\"TeNFi\"]},{\"battleId\":\"acb21ccaeb23c314\",\"battleMode\":\"CTF\",\"map\":\"map_island\",\"maxPeople\":1,\"name\":\"Island CTF\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":9,\"maxRank\":16,\"preview\":538875,\"suspicious\":false,\"usersBlue\":[\"des9ltkov\"],\"usersRed\":[\"Prior\"]},{\"battleId\":\"59d0b2acf9e8802b\",\"battleMode\":\"CTF\",\"map\":\"map_island\",\"maxPeople\":3,\"name\":\"Остров CTF\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":9,\"maxRank\":15,\"preview\":538875,\"suspicious\":false,\"usersBlue\":[\"aleks2579\",\"Legend_O-N-E\",\"Polyana\"],\"usersRed\":[\"KPUT\",\"TpyAkyJla\",\"Prince_Of_RailGun\"]},{\"battleId\":\"6757d506cf29d29b\",\"battleMode\":\"CTF\",\"map\":\"map_serpuhov\",\"maxPeople\":10,\"name\":\"Серпухов CTF\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":4,\"maxRank\":8,\"preview\":303326,\"suspicious\":false,\"usersBlue\":[\"P_E_T_P_O\"],\"usersRed\":[\"TIcuxaTIaT\"]},{\"battleId\":\"ffcd251fcc756d6e\",\"battleMode\":\"CP\",\"map\":\"map_polygon\",\"maxPeople\":8,\"name\":\"qartvelebo moit \",\"privateBattle\":false,\"proBattle\":false,\"minRank\":5,\"maxRank\":9,\"preview\":891846,\"suspicious\":false,\"usersBlue\":[\"vgvg644\",\"1234raketa\",\"DaDrunk\",\"wassimthekiller\",\"M_O_N_A_S_H_K_A\",\"Mike_Tyson\",\"Marsi_2015\",\"REZZ777\"],\"usersRed\":[\"Sova\",\"0002gabor\",\"Gr1nCH\",\"ZyynSX\",\"mattwolf\",\"leandro\"]},{\"battleId\":\"5480f4d421c4df0e\",\"battleMode\":\"CTF\",\"map\":\"map_platform\",\"maxPeople\":5,\"name\":\"Стрим WebSter и Zzeress\",\"privateBattle\":false,\"proBattle\":false,\"minRank\":5,\"maxRank\":12,\"preview\":431795,\"suspicious\":false,\"usersBlue\":[\"kwnziinho\",\"nkvd\",\"DENR_FX\",\"ado\"],\"usersRed\":[\"zzeress\",\"SOAD\",\"Waifu\",\"Mozantiny\"]},{\"battleId\":\"e85a3b80b4e6bae3\",\"battleMode\":\"CTF\",\"map\":\"map_silence\",\"maxPeople\":10,\"name\":\"Тишина CTF\",\"privateBattle\":false,\"proBattle\":true,\"minRank\":8,\"maxRank\":15,\"preview\":335175,\"suspicious\":false,\"usersBlue\":[],\"usersRed\":[]},{\"battleId\":\"7d1ad7e732b33147\",\"battleMode\":\"CTF\",\"map\":\"map_island\",\"maxPeople\":3,\"name\":\"wasp and fire\",\"privateBattle\":false,\"proBattle\":true,\"minRank\":9,\"maxRank\":16,\"preview\":538875,\"suspicious\":false,\"usersBlue\":[\"S_Y_N_E_R_G_Y\"],\"usersRed\":[\"A_Viuva_Preta_Br\"]}]}"
          json.adapter(InitBattleSelectData::class.java).toJson(
            InitBattleSelectData(
              battles = listOf(
                BattleData(
                  battleId = "1",
                  battleMode = "DM",
                  map = "map_sandbox",
                  name = "ProTanki Server",
                  maxPeople = 8,
                  minRank = 0,
                  maxRank = 16,
                  preview = 618467,
                  users = listOf(
                    "Luminate"
                  )
                )
              )
            )
          )
        )
      )
    )
  }

  private suspend fun initClient() {
    // TODO(Assasans): Shit
    val langReader = File("D:/ProTankiServer/src/main/resources/lang/ru.json").bufferedReader()
    val lang = langReader.use { it.readText() }

    // TODO(Assasans): Shit
    val resourcesAuthReader = File("D:/ProTankiServer/src/main/resources/resources/auth.json").bufferedReader()
    val resourcesAuth = resourcesAuthReader.use { it.readText() }

    send(Command(CommandName.InitExternalModel, mutableListOf("http://localhost/")))
    send(
      Command(
        CommandName.InitRegistrationModel,
        mutableListOf(
          // "{\"bgResource\": 122842, \"enableRequiredEmail\": false, \"maxPasswordLength\": 100, \"minPasswordLength\": 1}"
          json.adapter(InitRegistrationModelData::class.java).toJson(
            InitRegistrationModelData(
              enableRequiredEmail = false
            )
          )
        )
      )
    )

    send(Command(CommandName.InitLocale, mutableListOf(lang)))
    send(Command(CommandName.LoadResources, mutableListOf(resourcesAuth, "2")))
    awaitDependency(2)

    send(Command(CommandName.MainResourcesLoaded))
  }
}

data class InitBonusesData(
  @Json val init_bonuses: List<Any> = listOf() // TOOD(Assasans)
)

data class InitBattleModelData(
  @Json val kick_period_ms: Int = 125000,
  @Json val map_id: String,
  @Json val mapId: Int,
  @Json val invisible_time: Int = 3500,
  @Json val spectator: Boolean = false,
  @Json val active: Boolean = true,
  @Json val dustParticle: Int = 110001,
  @Json val battleId: String,
  @Json val minRank: Int = 3,
  @Json val maxRank: Int = 9,
  @Json val skybox: String,
  @Json val sound_id: Int = 584396,
  @Json val map_graphic_data: String
)

data class BonusLightingData(
  @Json val attenuationBegin: Int = 100,
  @Json val attenuationEnd: Int = 500,
  @Json val color: Int,
  @Json val intensity: Int = 1,
  @Json val time: Int = 0
)

data class BonusData(
  @Json val lighting: BonusLightingData,
  @Json val id: String,
  @Json val resourceId: Int,
  @Json val lifeTime: Int = 30
)

data class InitBonusesDataData(
  @Json val bonuses: List<BonusData>,
  @Json val cordResource: Int = 1000065,
  @Json val parachuteInnerResource: Int = 170005,
  @Json val parachuteResource: Int = 170004,
  @Json val pickupSoundResource: Int = 269321
)

data class ShowFriendsModalData(
  @Json val new_incoming_friends: List<FriendEntry> = listOf(),
  @Json val new_accepted_friends: List<FriendEntry> = listOf()
)

data class BattleUser(
  @Json val user: String,
  @Json val kills: Int = 0,
  @Json val score: Int = 0,
  @Json val suspicious: Boolean = false
)

abstract class ShowBattleInfoData(
  @Json val itemId: String,
  @Json val battleMode: String,
  @Json val scoreLimit: Int,
  @Json val timeLimitInSec: Int,
  @Json val preview: Int,
  @Json val maxPeopleCount: Int,
  @Json val name: String,
  @Json val proBattle: Boolean = false,
  @Json val minRank: Int,
  @Json val maxRank: Int,
  @Json val roundStarted: Boolean = true,
  @Json val spectator: Boolean,
  @Json val withoutBonuses: Boolean,
  @Json val withoutCrystals: Boolean,
  @Json val withoutSupplies: Boolean,
  @Json val proBattleEnterPrice: Int = 150,
  @Json val timeLeftInSec: Int,
  @Json val userPaidNoSuppliesBattle: Boolean = false,
  @Json val proBattleTimeLeftInSec: Int = -1
)

class ShowTeamBattleInfoData(
  itemId: String,
  battleMode: String,
  scoreLimit: Int,
  timeLimitInSec: Int,
  preview: Int,
  maxPeopleCount: Int,
  name: String,
  proBattle: Boolean = false,
  minRank: Int,
  maxRank: Int,
  roundStarted: Boolean = true,
  spectator: Boolean,
  withoutBonuses: Boolean,
  withoutCrystals: Boolean,
  withoutSupplies: Boolean,
  proBattleEnterPrice: Int = 150,
  timeLeftInSec: Int,
  userPaidNoSuppliesBattle: Boolean = false,
  proBattleTimeLeftInSec: Int = -1,

  @Json val usersRed: List<BattleUser>,
  @Json val usersBlue: List<BattleUser>,

  @Json val scoreRed: Int = 0,
  @Json val scoreBlue: Int = 0,

  @Json val autoBalance: Boolean,
  @Json val friendlyFire: Boolean,
) : ShowBattleInfoData(
  itemId,
  battleMode,
  scoreLimit,
  timeLimitInSec,
  preview,
  maxPeopleCount,
  name,
  proBattle,
  minRank,
  maxRank,
  roundStarted,
  spectator,
  withoutBonuses,
  withoutCrystals,
  withoutSupplies,
  proBattleEnterPrice,
  timeLeftInSec,
  userPaidNoSuppliesBattle,
  proBattleTimeLeftInSec
)

class ShowDmBattleInfoData(
  itemId: String,
  battleMode: String,
  scoreLimit: Int,
  timeLimitInSec: Int,
  preview: Int,
  maxPeopleCount: Int,
  name: String,
  proBattle: Boolean = false,
  minRank: Int,
  maxRank: Int,
  roundStarted: Boolean = true,
  spectator: Boolean,
  withoutBonuses: Boolean,
  withoutCrystals: Boolean,
  withoutSupplies: Boolean,
  proBattleEnterPrice: Int = 150,
  timeLeftInSec: Int,
  userPaidNoSuppliesBattle: Boolean = false,
  proBattleTimeLeftInSec: Int = -1,

  @Json val users: List<BattleUser>,
  @Json val score: Int = 0,
) : ShowBattleInfoData(
  itemId,
  battleMode,
  scoreLimit,
  timeLimitInSec,
  preview,
  maxPeopleCount,
  name,
  proBattle,
  minRank,
  maxRank,
  roundStarted,
  spectator,
  withoutBonuses,
  withoutCrystals,
  withoutSupplies,
  proBattleEnterPrice,
  timeLeftInSec,
  userPaidNoSuppliesBattle,
  proBattleTimeLeftInSec
)

data class BattleData(
  @Json val battleId: String,
  @Json val battleMode: String,
  @Json val map: String,
  @Json val maxPeople: Int,
  @Json val name: String,
  @Json val privateBattle: Boolean = false,
  @Json val proBattle: Boolean = false,
  @Json val minRank: Int,
  @Json val maxRank: Int,
  @Json val preview: Int,
  @Json val suspicious: Boolean = false,
  @Json val users: List<String>
)

data class InitBattleSelectData(
  @Json val battles: List<BattleData>
)

data class BattleLimit(
  @Json val battleMode: String,
  @Json val scoreLimit: Int,
  @Json val timeLimitInSec: Int,
)

data class Map(
  @Json val enabled: Boolean = true,
  @Json val mapId: String,
  @Json val mapName: String,
  @Json val maxPeople: Int,
  @Json val preview: Int,
  @Json val maxRank: Int,
  @Json val minRank: Int,
  @Json val supportedModes: List<String>,
  @Json val theme: String
)

data class InitBattleCreateData(
  @Json val maxRangeLength: Int = 7,
  @Json val battleCreationDisabled: Boolean = false,
  @Json val battleLimits: List<BattleLimit>,
  @Json val maps: List<Map>
)

data class ShowAchievementsData(
  @Json val ids: List<Int>
)

data class ChatMessage(
  @Json val name: String,
  @Json val rang: Int,
  @Json val chatPermissions: Int = 0,
  @Json val message: String,
  @Json val addressed: Boolean = false,
  @Json val chatPermissionsTo: Int = 0,
  @Json val nameTo: String = "",
  @Json val rangTo: Int = 0,
  @Json val system: Boolean = false,
  @Json val sourceUserPremium: Boolean = false,
  @Json val targetUserPremium: Boolean = false
)

data class InitChatMessagesData(
  @Json val messages: List<ChatMessage>
)

data class InitChatSettings(
  @Json val antiFloodEnabled: Boolean = true,
  @Json val typingSpeedAntifloodEnabled: Boolean = true,
  @Json val bufferSize: Int = 60,
  @Json val minChar: Int = 60,
  @Json val minWord: Int = 5,
  @Json val showLinks: Boolean = true,
  @Json val admin: Boolean = false,
  @Json val selfName: String = "roflanebalo",
  @Json val chatModeratorLevel: Int = 0,
  @Json val symbolCost: Int = 176,
  @Json val enterCost: Int = 880,
  @Json val chatEnabled: Boolean = true,
  @Json val linksWhiteList: List<String> = "http://gtanks-online.com/|http://vk.com/ebal"
    .toCharArray()
    .map(Char::toString)
)

data class AuthData(
  @Json val captcha: String,
  @Json val remember: Boolean,
  @Json val login: String,
  @Json val password: String
)

data class InitRegistrationModelData(
  @Json val bgResource: Int = 122842,
  @Json val enableRequiredEmail: Boolean = false,
  @Json val maxPasswordLength: Int = 100,
  @Json val minPasswordLength: Int = 1
)

data class InitPremiumData(
  @Json val left_time: Int = -1,
  @Json val needShowNotificationCompletionPremium: Boolean = false,
  @Json val needShowWelcomeAlert: Boolean = false,
  @Json val reminderCompletionPremiumTime: Int = 86400,
  @Json val wasShowAlertForFirstPurchasePremium: Boolean = false,
  @Json val wasShowReminderCompletionPremium: Boolean = true
)

data class InitPanelData(
  @Json val name: String = "roflanebalo",
  @Json val crystall: Int = 32,
  @Json val email: String? = null,
  @Json val tester: Boolean = false,
  @Json val next_score: Int = 3700,
  @Json val place: Int = 0,
  @Json val rang: Int = 4,
  @Json val rating: Int = 1,
  @Json val score: Int = 2307,
  @Json val currentRankScore: Int = 1500,
  @Json val hasDoubleCrystal: Boolean = false,
  @Json val durationCrystalAbonement: Int = -1,
  @Json val userProfileUrl: String = "http://ratings.generaltanks.com/ru/user/"
)

data class FriendEntry(
  @Json val id: String,
  @Json val rank: Int,
  @Json val online: Boolean
)

data class InitFriendsListData(
  @Json val friends: List<FriendEntry> = listOf(),
  @Json val incoming: List<FriendEntry> = listOf(),
  @Json val outcoming: List<FriendEntry> = listOf(),
  @Json val new_incoming_friends: List<FriendEntry> = listOf(),
  @Json val new_accepted_friends: List<FriendEntry> = listOf()
)
