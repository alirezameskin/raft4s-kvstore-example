
## KvStore
A simple Key/Value store with https://github.com/alirezameskin/raft4s/.

Building: 

```shell
sbt assembly
```

Running a cluster of three members:
```shell
#running instance 1
java -jar target/scala-2.13/raft4s-kvstore-example-assembly-0.1.jar --http-port 8080 --local localhost:8090 --member localhost:8091 --member localhost:8092
```
```shell
#running instance 2
java -jar target/scala-2.13/raft4s-kvstore-example-assembly-0.1.jar --http-port 8081 --local localhost:8091 --member localhost:8090 --member localhost:8092
```

```shell
#running instance 2
java -jar target/scala-2.13/raft4s-kvstore-example-assembly-0.1.jar --http-port 8082 --local localhost:8092 --member localhost:8090 --member localhost:8091
```


Calling endpoints:

```shell
#Setting a value to instance 1
curl --request POST --url http://localhost:8080/keys/name --data Alirerza

# Getting the data by calling on other nodes
curl --request GET --url http://localhost:8081/keys/name 

curl --request GET --url http://localhost:8082/keys/name 
```