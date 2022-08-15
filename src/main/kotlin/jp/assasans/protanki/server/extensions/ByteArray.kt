package jp.assasans.protanki.server.extensions

fun ByteArray.indexOfSequence(sequence: ByteArray, startFrom: Int = 0): Int {
  if(this.isEmpty()) return -1
  if(sequence.isEmpty()) return 0
  if(startFrom < 0) throw IllegalArgumentException("startFrom must be >= 0")
  if(startFrom >= this.size) return -1

  var matchOffset = 0
  var start = startFrom
  var offset = start
  while(offset < size) {
    if(this[offset] == sequence[matchOffset]) {
      if(matchOffset++ == 0) start = offset
      if(matchOffset == sequence.size) return start
    } else {
      matchOffset = 0
    }
    offset++
  }

  return -1
}
