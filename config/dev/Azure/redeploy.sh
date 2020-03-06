cd ~/ShieldCorda
# brind latest changes
git pull origin master
#Stop nodes
sudo systemctl stop cordaIssuer
sudo systemctl stop cordaTrader1
sudo systemctl stop cordaTrader2
sudo systemctl stop cordaBNO
sudo systemctl stop cordaNotary
# stop rest
sudo systemctl stop cordaREST
# deploy new nodes
./gradlew deployNodes
#copy config files
cp ./build/resources/main/membership-service.conf ./build/nodes/issuer/cordapps/config/
cp ./build/resources/main/membership-service.conf ./build/nodes/trader1/cordapps/config/
cp ./build/resources/main/membership-service.conf ./build/nodes/trader2/cordapps/config/
cp ./build/resources/main/membership-service.conf ./build/nodes/BNO/cordapps/config/
cp ./build/resources/main/membership-service.conf ./build/nodes/custodian/cordapps/config/
#start nodes
sudo systemctl start cordaIssuer
sudo systemctl start cordaTrader1
sudo systemctl start cordaTrader2
sudo systemctl start cordaBNO
sudo systemctl start cordaNotary
#Start rest
sudo systemctl start cordaREST
