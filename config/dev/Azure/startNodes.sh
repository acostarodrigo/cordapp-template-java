cp build/resources/main/membership-service.conf ../../../build/nodes/issuer/cordapp/config
cp build/resources/main/membership-service.conf ../../../build/nodes/trader1/cordapp/config
cp build/resources/main/membership-service.conf ../../../build/nodes/trader2/cordapp/config
cp build/resources/main/membership-service.conf ../../../build/nodes/BNO/cordapp/config
sudo systemctl start cordaIssuer
sudo systemctl start cordaTrader1
sudo systemctl start cordaTrader2
sudo systemctl start cordaBNO
sudo systemctl start cordaNotary
