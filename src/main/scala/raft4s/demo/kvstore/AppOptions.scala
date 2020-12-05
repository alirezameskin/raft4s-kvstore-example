package raft4s.demo.kvstore

import raft4s.Address

case class AppOptions(httpPort: Int, local: Address, servers: List[Address])
