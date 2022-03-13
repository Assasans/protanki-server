package jp.assasans.protanki.server.client

enum class UserRank(val value: Int, val score: Int) {
  Recruit(1, 0),
  Private(2, 100),
  Gefreiter(3, 500),
  Corporal(4, 1500),
  MasterCorporal(5, 3700),
  Sergeant(6, 7100),
  StaffSergeant(7, 12300),
  MasterSergeant(8, 20000),
  FirstSergeant(9, 29000),
  SergeantMajor(10, 41000),
  WarrantOfficer1(11, 57000),
  WarrantOfficer2(12, 76000),
  WarrantOfficer3(13, 98000),
  WarrantOfficer4(14, 125000),
  WarrantOfficer5(15, 156000),
  ThirdLieutenant(16, 192000),
  SecondLieutenant(17, 233000),
  FirstLieutenant(18, 280000),
  Captain(19, 332000),
  Major(20, 390000),
  LieutenantColonel(21, 455000),
  Colonel(22, 527000),
  Brigadier(23, 606000),
  MajorGeneral(24, 692000),
  LieutenantGeneral(25, 787000),
  General(26, 889000),
  Marshal(27, 1000000),
  FieldMarshal(28, 1122000),
  Commander(29, 1255000),
  Generalissimo(30, 1400000);

  companion object {
    private val map = values().associateBy(UserRank::value)

    fun get(index: Int) = map[index]
  }
}
