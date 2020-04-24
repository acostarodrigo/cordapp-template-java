# Node 1
# Treasurer
# API REST
ssh -i cordaAzure acostarodrigo@23.99.231.7

# Node 2
# BNO
# Notary
# Custodian
ssh -i cordaAzure rodrigo@13.89.63.69

# Node 3
# Issuer
# Trader1
# Trader2
ssh -i cordaAzure acostarodrigo@13.89.226.189

# reset dir
rm -dr build/nodes/
mkdir build/nodes
ls build/nodes

## copy to nodes
# Node 1
scp -i cordaAzure -r ~/invectorIQ/projects/securitize/sourceCode/cordapp-shield-java/build/nodes/treasurer acostarodrigo@23.99.231.7:/home/acostarodrigo/ShieldCorda/build/nodes/treasurer


# Node 2
scp -i cordaAzure -r ~/invectorIQ/projects/securitize/sourceCode/cordapp-shield-java/build/nodes/BNO rodrigo@13.89.63.69:/home/rodrigo/ShieldCorda/build/nodes/BNO
scp -i cordaAzure -r ~/invectorIQ/projects/securitize/sourceCode/cordapp-shield-java/build/nodes/custodian/ rodrigo@13.89.63.69:/home/rodrigo/ShieldCorda/build/nodes/custodian
scp -i cordaAzure -r ~/invectorIQ/projects/securitize/sourceCode/cordapp-shield-java/build/nodes/Notary/ rodrigo@13.89.63.69:/home/rodrigo/ShieldCorda/build/nodes/Notary

# Node 3
scp -i cordaAzure -r ~/invectorIQ/projects/securitize/sourceCode/cordapp-shield-java/build/nodes/issuer/ acostarodrigo@13.89.226.189:/home/acostarodrigo/ShieldCorda/build/nodes/issuer
scp -i cordaAzure -r ~/invectorIQ/projects/securitize/sourceCode/cordapp-shield-java/build/nodes/trader1/ acostarodrigo@13.89.226.189:/home/acostarodrigo/ShieldCorda/build/nodes/trader1
scp -i cordaAzure -r ~/invectorIQ/projects/securitize/sourceCode/cordapp-shield-java/build/nodes/trader2/ acostarodrigo@13.89.226.189:/home/acostarodrigo/ShieldCorda/build/nodes/trader2
