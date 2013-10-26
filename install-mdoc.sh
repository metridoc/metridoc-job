#!/bin/sh

INSTALL_DIR="$HOME/.metridoc/cli/install"

if [[ $1 ]];
then
    MDOC_VERSION=$1
    echo "searching for version $1"
else
    MDOC_VERSION=`curl -s https://api.bintray.com/packages/upennlib/metridoc-distributions/metridoc-job-cli | sed 's/.*latest_version":"\([^"]*\).*/\1/g'`
    echo "no version provided, installing latest version [$MDOC_VERSION]"
fi

if [[ -d $INSTALL_DIR ]];
then
    if grep -qF $MDOC_VERSION $INSTALL_DIR/mdoc/MDOC_VERSION; then
        echo "mdoc is up to date with version $MDOC_VERSION"
        exit 0
    fi
    echo "previous installation exists, deleting now"
    rm -rf $INSTALL_DIR
fi

mkdir -p $INSTALL_DIR

BINTRAY_URL="http://dl.bintray.com/upennlib/metridoc-distributions/mdoc-$MDOC_VERSION.zip"
DIST_FILE="mdoc-$MDOC_VERSION.zip"
DIST_LOCATION="$INSTALL_DIR/$DIST_FILE"

echo "downloading source file from [$BINTRAY_URL] to [$DIST_LOCATION]"
curl -L "$BINTRAY_URL" > "$DIST_LOCATION"
cd "$INSTALL_DIR"
unzip -q "$DIST_FILE"
mv "mdoc-$MDOC_VERSION" "mdoc"

MDOC_BIN="$INSTALL_DIR/mdoc/bin"
if ! grep -q 'cli/install/mdoc' "$HOME/.bash_profile"; then
    echo "\n" >> "$HOME/.bash_profile"
    echo "export PATH=$MDOC_BIN:\$PATH" >> "$HOME/.bash_profile"
fi

if ! grep -q 'cli/install/mdoc' "$HOME/.bashrc"; then
    echo "\n" >> "$HOME/.bashrc"
    echo "export PATH=$MDOC_BIN:\$PATH" >> "$HOME/.bashrc"
fi

cd "$MDOC_BIN"
./mdoc install-deps
echo "$MDOC_VERSION" > "$INSTALL_DIR/mdoc/MDOC_VERSION"

echo ""
echo "if this is a first time install, please open a new terminal"
echo ""