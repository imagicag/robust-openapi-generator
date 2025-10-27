#!/usr/bin/env bash
export VERSION=$1

cd target
rm *.md5
rm *.sha1
md5sum robust-openapi-generator-$VERSION.pom | awk '{print $1}' > robust-openapi-generator-$VERSION.pom.md5
sha1sum robust-openapi-generator-$VERSION.pom | awk '{print $1}' > robust-openapi-generator-$VERSION.pom.sha1
md5sum robust-openapi-generator-$VERSION.jar | awk '{print $1}' > robust-openapi-generator-$VERSION.jar.md5
sha1sum robust-openapi-generator-$VERSION.jar | awk '{print $1}' > robust-openapi-generator-$VERSION.jar.sha1
md5sum robust-openapi-generator-$VERSION-sources.jar | awk '{print $1}' > robust-openapi-generator-$VERSION-sources.jar.md5
sha1sum robust-openapi-generator-$VERSION-sources.jar | awk '{print $1}' > robust-openapi-generator-$VERSION-sources.jar.sha1
md5sum robust-openapi-generator-$VERSION-javadoc.jar | awk '{print $1}' > robust-openapi-generator-$VERSION-javadoc.jar.md5
sha1sum robust-openapi-generator-$VERSION-javadoc.jar | awk '{print $1}' > robust-openapi-generator-$VERSION-javadoc.jar.sha1

cd ..

rm -rf deploy
mkdir -p deploy/ch/imagic/robust-openapi-generator/$VERSION
cp target/robust-openapi-generator* deploy/ch/imagic/robust-openapi-generator/$VERSION/

#We dont publish this to maven central.
rm -f deploy/ch/imagic/robust-openapi-generator/$VERSION/robust-openapi-generator-0.1.0-jar-with-dependencies.jar
rm -f deploy/ch/imagic/robust-openapi-generator/$VERSION/robust-openapi-generator-0.1.0-jar-with-dependencies.jar.asc

cd deploy
zip -r b.zip ch