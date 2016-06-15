#!/bin/bash

SBT_OPTS="-Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=384M"
#java -Dfile.encoding=UTF-8  $SBT_OPTS -jar `dirname $0`/sbt-launch.jar "$@"

sbtver=0.13.9
sbtjar=sbt-launch.jar
sbtsha128=1de48c2c412fffc4336e4d7dee224927a96d5abc

sbtrepo=http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch

if [ ! -f $sbtjar ]; then
  set -x
  echo "downloading $sbtjar" 1>&2
  if ! curl --silent -L --fail --remote-name $sbtrepo/$sbtver/$sbtjar; then
    exit 1
  fi
  echo "download complete..."
fi

checksum=`openssl dgst -sha1 $sbtjar | awk '{ print $2 }'`
if [ "$checksum" != $sbtsha128 ]; then
  echo "bad $sbtjar.  delete $sbtjar and run $0 again."
  exit 1
fi

[ -f ~/.sbtconfig ] && . ~/.sbtconfig


if [ "$JAVA_HOME" == "" ]; then
    echo "[WARN] Java Home was not set, automatically setting it up"
    export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:jre/bin/java::")
fi

echo "Using JAVA : $JAVA_HOME"

JAVA="$JAVA_HOME/bin/java"


$JAVA -ea                          \
  $SBT_OPTS                       \
  $JAVA_OPTS                      \
  -Djava.net.preferIPv4Stack=true \
  -XX:+AggressiveOpts             \
  -XX:+UseParNewGC                \
  -XX:+UseConcMarkSweepGC         \
  -XX:+CMSParallelRemarkEnabled   \
  -XX:+CMSClassUnloadingEnabled   \
  -XX:ReservedCodeCacheSize=128m  \
  -XX:MaxPermSize=1024m           \
  -XX:SurvivorRatio=128           \
  -XX:MaxTenuringThreshold=0      \
  -Xss8M                          \
  -Xms512M                        \
  -Xmx8G                          \
  -server                         \
  -jar $sbtjar "$@"