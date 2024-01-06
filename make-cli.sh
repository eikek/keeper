#!/usr/bin/env bash

# build the application, which is a bit involved.
#
# first, just compile to catch any errors and have next steps run a
# bit quicker. Then build the webclient, which is wrapped into a
# webjar and put into the resources of the server module so it can
# be served from there. At last the cli is build.
#
# requires: sbt, npm, java (or run via nix)

set -o errexit -o pipefail -o noclobber -o nounset

export SBT_OPTS="-Xmx2G"

tdir="$(pwd)"
wdir=$(realpath "$(dirname "$0")")
cd $wdir

# compile the project
sbt clean compile

# create the webclient
cd $wdir/modules/webview
export KEEPER_BUILD_PROD=true
npm install && npm run build

# create the webjar
echo "Create webjar ..."
rm -rf keeper-webview-client.jar webjar/
mkdir -p webjar/META-INF/resources/webjars/keeper-webview
cp -r dist/* webjar/META-INF/resources/webjars/keeper-webview/
cd webjar
jar -c --file keeper-webview-client.jar *
mv keeper-webview-client.jar ..
cd $wdir/modules/webview
rm -rf webjar

# add the webjar as dependency to the server and build the cli
echo "Build the CLI"
mkdir -p ../server/lib/
cp keeper-webview-client.jar ../server/lib/

cd "$wdir"
sbt make-package
cd "$tdir"
echo "Done"
