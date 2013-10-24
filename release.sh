#!/bin/sh

systemCall() {
    echo "running $1"
    if eval $1; then
		echo "command [$1] ran successfully"
	else
		echo "command [$1] failed with exit status [$?]"
		exit 1
	fi
}

if grep -q "\-SNAPSHOT" "VERSION"; then
    echo "VERSION file has SNAPSHOT in it, skipping release"
    exit 0
fi

systemCall "./gradlew test integTests"

#releases to github
PROJECT_VERSION=`cat VERSION`
echo ""
echo "Releasing ${PROJECT_VERSION} to GitHub"
echo ""

systemCall "git tag -a v${PROJECT_VERSION} -m 'tagging release'"
systemCall "git push origin v${PROJECT_VERSION}"

#release to bintray
echo ""
echo "Releasing ${PROJECT_VERSION} to BinTray"
echo ""

systemCall "./gradlew publishArchives publishDistribution bumpVersion"
systemCall "git add VERSION"
systemCall "git commit -m 'committing a new version'"
systemCall "git push origin master"