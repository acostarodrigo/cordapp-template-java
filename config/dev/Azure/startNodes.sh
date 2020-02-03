cp ../../../build/resources/main/membership-service.conf ../../../build/nodes/issuer/cordapps/config/
cp ../../../build/resources/main/membership-service.conf ../../../build/nodes/trader1/cordapps/config/
cp ../../../build/resources/main/membership-service.conf ../../../build/nodes/trader2/cordapps/config/
cp ../../../build/resources/main/membership-service.conf ../../../build/nodes/BNO/cordapps/config/
sudo systemctl start cordaIssuer
sudo systemctl start cordaTrader1
sudo systemctl start cordaTrader2
sudo systemctl start cordaBNO
sudo systemctl start cordaNotary
