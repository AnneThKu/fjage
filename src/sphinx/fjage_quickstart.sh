#!/bin/sh

# fjage version
VERSION=1.3.4

# create the folder structure
mkdir -p build/libs etc logs samples

# download necessary JARs
cd build/libs
curl -O https://repo1.maven.org/maven2/com/github/org-arl/fjage/$VERSION/fjage-$VERSION.jar
curl -O https://repo1.maven.org/maven2/org/codehaus/groovy/groovy-all/2.4.4/groovy-all-2.4.4.jar
curl -O https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.1/commons-lang3-3.1.jar
curl -O https://repo1.maven.org/maven2/jline/jline/2.12/jline-2.12.jar
#curl -O https://repo1.maven.org/maven2/uk/com/robust-it/cloning/1.9.0/cloning-1.9.0.jar
#curl -O https://repo1.maven.org/maven2/org/objenesis/objenesis/1.2/objenesis-1.2.jar
cd ../..

# download init scripts and logging configuration
cd etc
curl -O https://raw.githubusercontent.com/org-arl/fjage/master/etc/initrc.groovy
cd ..

# download sample agents
cd samples
curl -O https://raw.githubusercontent.com/org-arl/fjage/master/samples/01_hello.groovy
curl -O https://raw.githubusercontent.com/org-arl/fjage/master/samples/02_ticker.groovy
curl -O https://raw.githubusercontent.com/org-arl/fjage/master/samples/03_weatherStation.groovy
curl -O https://raw.githubusercontent.com/org-arl/fjage/master/samples/03_weatherRequest.groovy
curl -O https://raw.githubusercontent.com/org-arl/fjage/master/samples/WeatherForecastReqMsg.groovy
curl -O https://raw.githubusercontent.com/org-arl/fjage/master/samples/04_weatherStation.groovy
curl -O https://raw.githubusercontent.com/org-arl/fjage/master/samples/04_weatherRequest.groovy
cd ..

# download startup script
curl -O https://raw.githubusercontent.com/org-arl/fjage/master/fjage.sh
chmod a+x fjage.sh
