package jp.assasans.protanki.server.client

enum class UserRank(val value: Int, val score: Int, val bonusCrystals: Int) {
  Recruit(1, 0, 0),
  Private(2, 100, 10),
  Gefreiter(3, 500, 40),
  Corporal(4, 1500, 120),
  MasterCorporal(5, 3700, 230),
  Sergeant(6, 7100, 420),
  StaffSergeant(7, 12300, 740),
  MasterSergeant(8, 20000, 950),
  FirstSergeant(9, 29000, 1400),
  SergeantMajor(10, 41000, 2000),
  WarrantOfficer1(11, 57000, 2500),
  WarrantOfficer2(12, 76000, 3100),
  WarrantOfficer3(13, 98000, 3900),
  WarrantOfficer4(14, 125000, 4600),
  WarrantOfficer5(15, 156000, 5600),
  ThirdLieutenant(16, 192000, 6600),
  SecondLieutenant(17, 233000, 7900),
  FirstLieutenant(18, 280000, 8900),
  Captain(19, 332000, 10000),
  Major(20, 390000, 12000),
  LieutenantColonel(21, 455000, 14000),
  Colonel(22, 527000, 16000),
  Brigadier(23, 606000, 17000),
  MajorGeneral(24, 692000, 20000),
  LieutenantGeneral(25, 787000, 22000),
  General(26, 889000, 24000),
  Marshal(27, 1000000, 28000),
  FieldMarshal(28, 1122000, 31000),
  Commander(29, 1255000, 34000),
  Generalissimo(30, 1400000, 37000);

  companion object {
    private val map = values().associateBy(UserRank::value)

    fun get(index: Int) = map[index]
  }
}

val UserRank.nextRank: UserRank?
  get() = UserRank.get(value + 1)

val UserRank?.scoreOrZero: Int
  get() = this?.score ?: 0
