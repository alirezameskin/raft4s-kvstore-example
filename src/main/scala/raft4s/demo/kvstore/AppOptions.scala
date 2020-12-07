package raft4s.demo.kvstore

import raft4s.Address

import java.io.File

case class AppOptions(storagePath: File, httpPort: Int, local: Address, servers: List[Address])
