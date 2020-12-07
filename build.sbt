name := "raft4s-kvstore-example"

version := "0.1"

scalaVersion := "2.13.4"

val http4sVersion = "0.21.6"
val grpcVersion   = "1.31.0"

libraryDependencies ++= Seq(
  "com.monovore"             %% "decline"             % "1.3.0",
  "com.monovore"             %% "decline-effect"      % "1.3.0",
  "dev.profunktor"           %% "console4cats"        % "0.8.1",
  "com.github.alirezameskin" %% "raft4s-core"         % "0.0.3",
  "com.github.alirezameskin" %% "raft4s-grpc"         % "0.0.3",
  "com.github.alirezameskin" %% "raft4s-rocksdb"      % "0.0.3",
  "com.github.valskalla"     %% "odin-core"           % "0.8.1",
  "org.http4s"               %% "http4s-dsl"          % http4sVersion,
  "org.http4s"               %% "http4s-circe"        % http4sVersion,
  "org.http4s"               %% "http4s-blaze-server" % http4sVersion,
  "org.http4s"               %% "http4s-blaze-client" % http4sVersion
)

resolvers += "raft4s".at("https://maven.pkg.github.com/alirezameskin/raft4s")

assemblyMergeStrategy in assembly := {
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case _                      => MergeStrategy.first
}

assemblyJarName := "kvstore.jar"
