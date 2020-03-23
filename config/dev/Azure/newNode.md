# New Node configuration steps

## install java
```shell script
sudo apt-get update
sudo apt-get upgrade
# reboot if neccesary
sudo apt install openjdk-8-jdk
```

## install gradle

```shell script
wget https://services.gradle.org/distributions/gradle-5.2.1-bin.zip -P /tmp
sudo unzip -d /opt/gradle /tmp/gradle-*.zip
```
set up env variables
```shell script
sudo vi /etc/profile.d/gradle.sh
export GRADLE_HOME=/opt/gradle/gradle-5.2.1
export PATH=${GRADLE_HOME}/bin:${PATH}
source /etc/profile.d/gradle.sh
```
verify
```shell script
gradle -v
```

## compile nodes

```shell script
cd ShieldCorda
gradle deployNodes
```
