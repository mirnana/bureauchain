# Bureauchain

This is a project in Hyperledger Fabric that illustrates how bureaucracy could be handled using blockchain technology. Only one type of document is represented, however one can conclude how this idea could be expanded to implement tracking all types of private documents.

## Chaincode and apps

We assume that Fabric's test-network is being used to run this project. The chaincode is written in Java and it enables creating, updating, reading and deleting assets of type Diploma. If CouchDB is used instead of the default LevelDB (which is recommended), one can read the assets by attributes other than the ID. 

There are two Gateway applications. `application-gateway-diploma` connects to test-network's Org1 and is meant to be used by users with the authority to create, change or delete college diplomas. Hence this application also connects to a local database to retreive data about students who are to receive a diploma. A simple structure of the database as given in `db/bureau-struct.sql` is assumed. 

The other application, `application-gateway-diploma-public` connects to test-network's Org2 and is meant to be used by the general public, as the diploma is a type of personal document that should be available for public verification. Users of this app are only allowed to read the diplomas and not make any changes to them. 

## Prerequisites

Docker, Go, Java and MySQL are required to run this project. Please follow [Fabric's instructions](https://hyperledger-fabric.readthedocs.io/en/latest/prereqs.html) to install the prerequisites for Fabric and make sure you have Java 11 and MySQL installed. 

When you clone this repository, navigate to its root and run the following commands to install Fabric: 

```bash
curl -sSLO https://raw.githubusercontent.com/hyperledger/fabric/main/scripts/install-fabric.sh && chmod +x install-fabric.sh
./install-fabric.sh --fabric-version 2.5.4
```
 
This will create a `fabric-samples` directory that contains all the necessary binaries, Docker images and some samples -- including the test-network.

Create a MySQL user with the username `user` and password `password`. ([See instructions.](https://dev.mysql.com/doc/refman/8.0/en/create-user.html)) Then create a database `bureau` with the four tables described in `db/bureau-struct.sql` and grant all privileges on them to `user`. MySQL should be running on its default port, 3306.

## Run the project

Firstly, start the test-network and create a channel:

```bash
cd fabric-samples/test-network/
./network.sh up createChannel -ca -s couchdb
```

This command runs the peers with CouchDB as the state database and Hyperledger's Certificate Authority. The name of the channel will default to `mychannel`. Now you can deploy the chaincode to the channel:

```bash
./network.sh deployCC -ccn diploma -ccp ../../chaincode-diploma/ -ccl java -ccep "OR('Org1MSP.peer','Org2MSP.peer')"
```

The flag `-ccep` sets the endorsement policy so that each organization's peer can create an asset without the other organization's endorsement. 

Once the chaincode is successfully deployed and you made sure that the MySQL database server is running, you can try out one or both the gateway apps. For example:

```bash
cd ../../application-gateway-diploma/
./gradlew run --console=plain
```

When you are done using the apps and the network, navigate back to `fabric-samples/test-network` and tear down the network:

```bash
cd ../fabric-samples/test-network/
./network.sh down
```

## References

- [Fabric Docs 2.5](https://hyperledger-fabric.readthedocs.io/en/release-2.5/index.html)
- [Fabric Chaincode API](https://hyperledger.github.io/fabric-chaincode-java/)
- [Fabric Gateway API](https://hyperledger.github.io/fabric-gateway/)
- [Fabric Samples](https://github.com/hyperledger/fabric-samples)