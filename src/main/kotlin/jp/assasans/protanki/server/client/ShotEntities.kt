package jp.assasans.protanki.server.client

// fire_target
// -> {"incarnation":8,"hitPoint":{"y":73.29619461332966,"z":72.72237265695335,"x":-49.36979967718842},"physTime":68814,"targetPosition":{"y":-297.0259233690682,"z":79.0416489003912,"x":2966.4105099433746},"targetPositionGlobal":{"y":-342.12333132025424,"z":154.1123709518328,"x":2892.7296899385924},"target":"4eJI_TbI","staticHitPosition":null}
// -> {"physTime":100693,"targetPositions":null,"incarnations":null,"targets":[],"hitPositions":[],"staticHitPosition":{"y":-773.8880089818483,"z":900,"x":-12586.573603396457}}

// target_shot
// <- {"physTime":1119900,"hitPositions":[{"y":-60.466048877439036,"z":129.9860431779512,"x":-1.0857645530845472}],"targets":["kartochka"],"incarnations":[39],"targetPositions":[{"y":-2453.38521203276,"z":977.7541997585313,"x":-11299.24064387775}],"staticHitPosition":{"y":-2577.9995742054452,"z":1109.9551781868727,"x":-11681.608120810322}}
// <- Mblwka;{"targetPosition":{"y":-737.213176235363,"z":1064.8728433271492,"x":-14024.694717948129},"physTime":3825507,"target":"Ok_2500","shotId":1072,"hitPoint":{"y":-687.920661900166,"z":1091.561345874681,"x":-13873.519580748303}}
// <- BEST_XR_FX;{"targetPositions":null,"physTime":4755480,"staticHitPosition":{"y":-7496.653214707261,"z":1714.5145739445807,"x":-6327.285948300254},"hitPositions":[],"targets":[],"incarnations":null};;
class FireTargetData(
  physTime: Int,
  control: Int,
  specificationID: Int,
  position: Vector3Data,
  linearVelocity: Vector3Data,
  orientation: Vector3Data,
  angularVelocity: Vector3Data
)
