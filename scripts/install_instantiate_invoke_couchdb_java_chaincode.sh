#!/bin/bash
GLOBAL_ENV_LOCATION=$PWD/scripts/.env
source $GLOBAL_ENV_LOCATION

set -ev

set -x

# ============================
# INSTALLING CHAINCODE IN ORG1
# ============================
docker exec "$CLI_NAME" peer chaincode install -l java -n "$COUCHDB_JAVA_CC_NAME" -p "$COUCHDB_JAVA_CC_SRC" -v v0

# ============================
# INSTALLING CHAINCODE IN ORG2
# ============================
docker exec -e "CORE_PEER_LOCALMSPID=Org2MSP" -e "CORE_PEER_TLS_CERT_FILE=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/ch.example.com/peers/peer0.org2.example.com/tls/server.crt" -e "CORE_PEER_TLS_KEY_FILE=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/server.key" -e "CORE_PEER_TLS_ROOTCERT_FILE=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp" -e "CORE_PEER_ADDRESS=peer0.org2.example.com:7051" "$CLI_NAME" peer chaincode install -l java -n "$COUCHDB_JAVA_CC_NAME" -p "$COUCHDB_JAVA_CC_SRC" -v v0

# ============================
# INSTALLING CHAINCODE IN ORG3
# ============================
docker exec -e "CORE_PEER_LOCALMSPID=Org3MSP" -e "CORE_PEER_TLS_CERT_FILE=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/ch.example.com/peers/peer0.org3.example.com/tls/server.crt" -e "CORE_PEER_TLS_KEY_FILE=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org3.example.com/peers/peer0.org3.example.com/tls/server.key" -e "CORE_PEER_TLS_ROOTCERT_FILE=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org3.example.com/peers/peer0.org3.example.com/tls/ca.crt" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org3.example.com/users/Admin@org3.example.com/msp" -e "CORE_PEER_ADDRESS=peer0.org3.example.com:7051" "$CLI_NAME" peer chaincode install -l java -n "$COUCHDB_JAVA_CC_NAME" -p "$COUCHDB_JAVA_CC_SRC" -v v0

# ===========================
# INSTANTIATING THE CHAINCODE
# ===========================
# add dummy employee with empID with 0
docker exec "$CLI_NAME" peer chaincode instantiate -o "$ORDERER_NAME":7050 -C "$CHANNEL_NAME" -l java -n "$COUCHDB_JAVA_CC_NAME" "$COUCHDB_JAVA_CC_SRC" -v v0  -c '{"Args":["init", "0", "init_name", "init_dept", "5", "init_loc"]}' -P "OR('Org1MSP.member', 'Org2MSP.member', 'Org3MSP.member' )" --tls --cafile $ORDERER_CA

sleep 3

# ================================
# LISTING THE CHAINCODES INSTALLED
# ================================
docker exec "$CLI_NAME" peer chaincode list --instantiated -C "$CHANNEL_NAME" --tls --cafile $ORDERER_CA

sleep 10

# ================================
# INVOKING CHAINCODE	(Adding multiple employees)
# ================================
# adds employee with given values
docker exec "$CLI_NAME" peer chaincode invoke -o "$ORDERER_NAME":7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $COUCHDB_JAVA_CC_NAME -c '{"Args":["addEmployee","1", "Kalyan", "Blockchain", "100", "Hyd"]}'
sleep 10

docker exec "$CLI_NAME" peer chaincode invoke -o "$ORDERER_NAME":7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $COUCHDB_JAVA_CC_NAME -c '{"Args":["addEmployee","2", "Fabric", "Hyperledger", "1004", "Delhi"]}'
sleep 10

docker exec "$CLI_NAME" peer chaincode invoke -o "$ORDERER_NAME":7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $COUCHDB_JAVA_CC_NAME -c '{"Args":["addEmployee","3", "Kalyan", "Blockchain", "15", "Hyd"]}'
sleep 10

docker exec "$CLI_NAME" peer chaincode invoke -o "$ORDERER_NAME":7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $COUCHDB_JAVA_CC_NAME -c '{"Args":["addEmployee","4", "Konda", "Fabric", "10003", "Blr"]}'

sleep 10

# ================================
# QUERING CHAINCODE - Normal regular query (query employee by id)
# ================================
docker exec "$CLI_NAME" peer chaincode invoke -o "$ORDERER_NAME":7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $COUCHDB_JAVA_CC_NAME -c '{"Args":["queryEmployee", "1"]}'

# ================================
# RICH QUERY: queryEmpBySalaryGreaterThanXAmount - Parameterized rich query
# ================================
docker exec "$CLI_NAME" peer chaincode invoke -o "$ORDERER_NAME":7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $COUCHDB_JAVA_CC_NAME -c '{"Args":["queryEmpBySalaryGreaterThanXAmount", "100"]}'

# ================================
# RICH QUERY: queryEmployees - Ad hoc rich query (query employees by name)
# ================================
docker exec "$CLI_NAME" peer chaincode invoke -o "$ORDERER_NAME":7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $COUCHDB_JAVA_CC_NAME -c '{"Args":["queryEmployees", "{\"selector\":{\"empName\":\"Kalyan\"}, \"use_index\":[\"_design/empNameIndexDoc\", \"empNameIndex\"]}"]}'

# ================================
# QUERING CHAINCODE - Normal regular query (query employees by given empIDs range)
# ================================
docker exec "$CLI_NAME" peer chaincode invoke -o "$ORDERER_NAME":7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $COUCHDB_JAVA_CC_NAME -c '{"Args":["queryEmpsByGivenEmpIDRange", "0", "3"]}'

# ================================
# RICH QUERY: queryEmpBySalaryGreaterThanXAmountWithPagination (get first 3 rows whose salary is greater than 1) - Parameterized rich query
# ================================
docker exec "$CLI_NAME" peer chaincode invoke -o "$ORDERER_NAME":7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $COUCHDB_JAVA_CC_NAME -c '{"Args":["queryEmpBySalaryGreaterThanXAmountWithPagination", "1", "3", ""]}'

# ================================
# INVOKING CHAINCODE - Update employee
# ================================
docker exec "$CLI_NAME" peer chaincode invoke -o "$ORDERER_NAME":7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $COUCHDB_JAVA_CC_NAME -c '{"Args":["updateEmployee", "1", "Raja", "AI", "89", "Bvrm"]}'
sleep 10

# ================================
# INVOKING CHAINCODE - Update employee
# ================================
docker exec "$CLI_NAME" peer chaincode invoke -o "$ORDERER_NAME":7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $COUCHDB_JAVA_CC_NAME -c '{"Args":["updateEmployee", "1", "Tinku", "Some", "59", "Pkl"]}'
sleep 10

# ================================
# QUERING CHAINCODE - Check the updates now	(Normal regular query (query employee by id))
# ================================
docker exec "$CLI_NAME" peer chaincode invoke -o "$ORDERER_NAME":7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $COUCHDB_JAVA_CC_NAME -c '{"Args":["queryEmployee", "1"]}'
sleep 10


# ================================
# QUERING CHAINCODE - Get history for a key now
# ================================
docker exec "$CLI_NAME" peer chaincode invoke -o "$ORDERER_NAME":7050 --tls --cafile $ORDERER_CA -C $CHANNEL_NAME -n $COUCHDB_JAVA_CC_NAME -c '{"Args":["getHistoryForKey", "1"]}'
