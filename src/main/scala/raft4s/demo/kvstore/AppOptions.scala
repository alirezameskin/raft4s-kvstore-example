package raft4s.demo.kvstore

import raft4s.Address

import java.nio.file.Path

case class AppOptions(storagePath: Path, httpPort: Int, local: Address, servers: List[Address])
