package fp.backend

import fp.{SiloRefId, Silo}

import scala.collection.concurrent.TrieMap

trait SiloWarehouse {

  /* Be careful, [[ConcurrentMap]] could be better than [[TrieMap]] */
  def silos: TrieMap[SiloRefId, Silo[_]]

}
