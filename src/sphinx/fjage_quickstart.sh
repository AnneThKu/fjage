#!/bin/sh

# create the folder structure
mkdir -p build/libs etc logs

# download necessary JARs
cd build/libs
curl -O http://search.maven.org/remotecontent?filepath=com/github/org-arl/fjage/1.0.1/fjage-1.0.1.jar
curl -O http://search.maven.org/remotecontent?filepath=org/codehaus/groovy/groovy-all/2.1.2/groovy-all-2.1.2.jar
curl -O http://search.maven.org/remotecontent?filepath=org/apache/commons/commons-lang3/3.1/commons-lang3-3.1.jar
curl -O http://search.maven.org/remotecontent?filepath=jline/jline/2.10/jline-2.10.jar
curl -O http://search.maven.org/remotecontent?filepath=uk/com/robust-it/cloning/1.9.0/cloning-1.9.0.jar
curl -O http://search.maven.org/remotecontent?filepath=org/objenesis/objenesis/1.2/objenesis-1.2.jar
cd ../..

# download init scripts and logging configuration
cd etc
curl -O https://raw.github.com/org-arl/fjage/master/etc/initrc.groovy
curl -O https://raw.github.com/org-arl/fjage/master/etc/logging.properties
curl -O https://raw.github.com/org-arl/fjage/master/etc/fjageshrc.groovy
cd ..

# download startup script
curl -O https://raw.github.com/org-arl/fjage/master/fjage.sh
chmod a+x fjage.sh
