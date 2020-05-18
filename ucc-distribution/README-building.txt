#
# Building UCC packages
#

You need Java and Apache Maven. 
Check the versions given in the pom.xml file. 
If not already done, build the jars from the root dir :

 cd .. ; mvn clean install -DskipTests

#
# Creating distribution packages
#

The following commands create the distribution packages
in tgz, deb and rpm formats (Maven 2!). The versions
are taken from the pom.xml

#tgz
 mvn package -DskipTests -Ppackman -Dpackage.type=bin.tar.gz
 
#deb
 mvn package -DskipTests -Ppackman -Dpackage.type=deb -Ddistribution=Debian

#rpm redhat
 mvn package -DskipTests -Ppackman -Dpackage.type=rpm -Ddistribution=RedHat



