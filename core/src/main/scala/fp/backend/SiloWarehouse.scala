package fp.backend

import fp.{SiloRefId, Silo}

trait SiloWarehouse {

  /* Be careful, [[ConcurrentMap]] could be better than [[TrieMap]] */
  def silos: scala.collection.mutable.Map[SiloRefId, Silo[_]]

}
