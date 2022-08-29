package jp.assasans.protanki.server.extensions

fun ByteArray.indexOfSequence(sequence: ByteArray, startFrom: Int = 0): Int {
  if(startFrom < 0) throw IllegalArgumentException("startFrom must be >= 0")
  if(this.isEmpty()) return -1
  if(sequence.isEmpty()) return 0
  if(startFrom + sequence.size >= this.size) return -1

  var index = startFrom
  var matchIndex = 0
  while(index < size) {
    val value = this[index++]

    if(value != sequence[matchIndex]) matchIndex = 0
    if(value == sequence[matchIndex]) matchIndex++

    if(matchIndex == sequence.size) return index - matchIndex
  }

  return -1
}
