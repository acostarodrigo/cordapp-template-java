# Azure configuration
## Dev Environment

Instructions

* clone repo https://github.com/acostarodrigo/ShieldCorda.git

```shell script
git clone https://github.com/acostarodrigo/ShieldCorda.git
```

* build nodes
```shell script
./gradlew buildNodes
```

* copy service files to enable automatic start of nodes
All .service files must be copied to /etc/systemd/system


Grant sudo access to files 

```shell script
sudo chown root:root /etc/systemd/system/corda*.service
sudo chmod 644 /etc/systemd/system/corda*.service
```

```shell script
sudo systemctl daemon-reload
sudo systemctl enable --now cordaIssuer
sudo systemctl enable --now cordaTrader1
sudo systemctl enable --now cordaTrader2
sudo systemctl enable --now cordaBNO
sudo systemctl enable --now cordaNotary
sudo systemctl enable --now cordaTreasurer
sudo systemctl enable --now cordaCustodian
```
