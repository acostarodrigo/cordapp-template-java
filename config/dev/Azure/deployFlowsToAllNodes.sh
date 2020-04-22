# build cordapp first!

# stop corda apps
ssh -i cordaAzure rodrigo@13.89.63.69 "sudo systemctl stop cordaNotary"
ssh -i cordaAzure rodrigo@13.89.63.69 "sudo systemctl stop cordaCustodian"
ssh -i cordaAzure acostarodrigo@13.89.226.189 "sudo systemctl stop cordaIssuer"
ssh -i cordaAzure acostarodrigo@13.89.226.189 "sudo systemctl stop cordaTrader1"
ssh -i cordaAzure acostarodrigo@13.89.226.189 "sudo systemctl stop cordaTrader2"

# copy
scp -i cordaAzure -r ~/invectorIQ/projects/securitize/sourceCode/cordapp-shield-java/build/nodes/issuer/cordapps/workflows-0.1.jar rodrigo@13.89.63.69:/home/rodrigo/ShieldCorda/build/nodes/treasurer/cordapps/
scp -i cordaAzure -r ~/invectorIQ/projects/securitize/sourceCode/cordapp-shield-java/build/nodes/issuer/cordapps/workflows-0.1.jar rodrigo@13.89.63.69:/home/rodrigo/ShieldCorda/build/nodes/custodian/cordapps/
scp -i cordaAzure -r ~/invectorIQ/projects/securitize/sourceCode/cordapp-shield-java/build/nodes/issuer/cordapps/workflows-0.1.jar acostarodrigo@13.89.226.189:/home/acostarodrigo/ShieldCorda/build/nodes/issuer/cordapps
scp -i cordaAzure -r ~/invectorIQ/projects/securitize/sourceCode/cordapp-shield-java/build/nodes/issuer/cordapps/workflows-0.1.jar acostarodrigo@13.89.226.189:/home/acostarodrigo/ShieldCorda/build/nodes/trader1/cordapps
scp -i cordaAzure -r ~/invectorIQ/projects/securitize/sourceCode/cordapp-shield-java/build/nodes/issuer/cordapps/workflows-0.1.jar acostarodrigo@13.89.226.189:/home/acostarodrigo/ShieldCorda/build/nodes/trader2/cordapps

# Restart corda nodes
ssh -i cordaAzure rodrigo@13.89.63.69 "sudo systemctl start cordaNotary"
ssh -i cordaAzure rodrigo@13.89.63.69 "sudo systemctl start cordaCustodian"
ssh -i cordaAzure acostarodrigo@13.89.226.189 "sudo systemctl start cordaIssuer"
ssh -i cordaAzure acostarodrigo@13.89.226.189 "sudo systemctl start cordaTrader1"
ssh -i cordaAzure acostarodrigo@13.89.226.189 "sudo systemctl start cordaTrader2"
