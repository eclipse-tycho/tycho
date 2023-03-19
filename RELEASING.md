# Releasing Tycho

This describes the steps to perform a release of Tycho:

## Prerequisites for performing a release

- [ ] Make sure you have everything setup (GPG installed!) for deploying to the Nexus OSS repository, see https://central.sonatype.org/pages/ossrh-guide.html guide
- [ ] Make sure you have a pgp key and it is published to the key-servers eg. https://keys.openpgp.org/
- [ ] Make sure you have a sonartype account and are allowed to publish for tycho organization
- [ ] Add your credentials for server `sonatype-nexus-staging` in `~/.m2/settings.xml`
```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
   <!-- ... -->
   <servers>
    <server>
      <id>sonatype-nexus-staging</id>
      <username>mickael.istria</username>
      <password>securePassword</password> <!-- use `mvn --encrypt-password` to not store plain text -->
    </server>
    <!-- ... -->
   </servers>
</settings>
</source>
```


## Making a new release for the current stream

** NEEDS UPDATE **

- [ ] Make sure all fixed issues and merged PRs have the correct milestone for this release, to finding PRs without milestone you can use the following filter `is:pr is:merged no:milestone` issues without milestone can be found with `is:issue no:milestone is:closed`
- [ ] Prepare the [release notes](https://github.com/eclipse-tycho/tycho/blob/master/RELEASE_NOTES.md) which should provide a quick overview of new features and bug fixes
- [ ] When no more incoming change is expected for this release, create branch `tycho-N.M.x` (e.g. `tycho-2.4.x`) for upcoming release and push it to remote; this branch should remain frozen until the release, only major fixes for regressions could be merged in before release. Work can still happen regularly for the following version on the `master` branch.
- [ ] Create release record on https://projects.eclipse.org/projects/technology.tycho projects.eclipse.org, link the N&N to https://github.com/eclipse-tycho/tycho/blob/[branch-name]/RELEASE_NOTES.md release notes]
- [ ] Update the Jenkinsfile on the `tycho-N.M.x` and adjust the `stage('Deploy Snapshot')` to reference the new branch in the when conditional.
- [ ] Update versions on `master `to future release with `mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=<NEXT_VERSION>-SNAPSHOT` and push to remote
- [ ] Announce the intent to release and request feedback about snapshots on the [GitHub discussions](https://github.com/eclipse-tycho/tycho/discussions):
```
Subject: Tycho <VERSION> release

We plan to release Tycho <VERSION> next week. For details of new features and bugfixes, see [release notes](https://github.com/eclipse-tycho/tycho/blob/tycho-x.y.z/RELEASE_NOTES.md).
Please help by testing the SNAPSHOTS build. To use it, change your tycho version to <VERSION>-SNAPSHOT and add the following snippet to your pom.

<pluginRepositories>
    <pluginRepository>
      <id>tycho-snapshots</id>
      <url>https://repo.eclipse.org/content/repositories/tycho-snapshots/</url>
    </pluginRepository>
</pluginRepositories>


We plan to promote this release in one week unless major regressions are found.

Regards,
Tycho team
```

... Wait until review date (usually a week later)...

- [ ] make sure all tags are fetched with `git fetch -t`
- [ ] `git fetch eclipse tycho-N.M.x && git checkout FETCH_HEAD` to get the branch to be released
- [ ] Update version to remove `-SNAPSHOT` with `mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=<VERSION>`
- [ ] Update versions in tycho-demo folder
- [ ] `git add * && git commit` version change
- [ ] Deploy to nexus staging repository (check that the correct pgp key is used and published to the key-servers eg. https://keys.openpgp.org/): 
```
mvn clean deploy -Prelease -DskipTests -Dsource=8 -DjdetectJavaApiLink=false \
     -DforgeReleaseId=sonatype-nexus-staging \
     -DforgeReleaseUrl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
     -Dgpg.keyname=<your e-mail of pgpg key>
```
- In the meantime...
    - [ ] `git fetch --all --tags`
    - [ ] `git tag tycho-<TYCHO_VERSION>`
    - [ ] Find out who contributed to the release:
```
git log --pretty=format:%an tycho-<previousVersion>..tycho-<newVersion> | sort | uniq
git log --grep="Also-[bB]y:" tycho-<previousVersion>..tycho-<newVersion> | grep -i also-by | sed -e 's/.*Also-[bB]y:\s*\(.*\)/\1/' | sort | uniq
```
   - [ ] Find out who has sponsored something in this release:
   ```
   is:closed is:issue label:sponsored milestone:<version mile stone> 
   ```
   - [ ] [Draft a release](https://github.com/eclipse-tycho/tycho/releases/new), thanking the contributors:
```
Subject: Tycho <VERSION> is released

Tycho <VERSION> has been released and is available from Maven Central repository.

🆕 https://github.com/eclipse-tycho/tycho/blob/master/RELEASE_NOTES.md#xyz
🏷️ https://github.com/eclipse-tycho/tycho/tree/tycho-x.y.z
👔 https://projects.eclipse.org/projects/technology.tycho/releases/x.y.z
🙏 contributors who contributed patches for this release:
    <contributors>
💰 we would like to also thank <list of sponsors> for sponsoring contributions in this release

and thanks to everyone who helped us with testing the snapshot version.

Regards,

 The Tycho Team

```
- [ ] close the staging repository on https://oss.sonatype.org/#stagingRepositories 
- [ ] Wait until all checks are done (this takes some time, see Activity tab). If all checks have passed, release the repository.
- [ ] Wait for artifacts to be available on Maven central, e.g. by looking at https://repo1.maven.org/maven2/org/eclipse/tycho/tycho-core/
- [ ] push tag `git push origin tycho-<TYCHO_VERSION>`
- Documentation
  - [ ] Generate site docs using `mvn install site site:stage -DskipTests` and check the result from `target/staging` seems viable.
  - [ ] Prepare documentation on the webite, using git repo https://git.eclipse.org/c/www.eclipse.org/tycho.git/ : copy the local site docs folders `target/staging/sitedocs` to the existing `sitedocs` folder and then submit as Gerrit review (don't merge yet)
- Announce the release
  - [ ] Publish the draft release you made above. This will also send an email to watchers of the project.
  - [ ] Forward the above email to tycho-dev@eclipse.org
  - [ ] Add a link to the above release on the discussion started earlier.
  - [ ] Optionally, announce on other medias (Twitter)
- [ ] Create, push and merge patch changing the bootstrap Tycho version for Tycho build (eg https://git.eclipse.org/r/c/tycho/org.eclipse.tycho/+/174964 )

## Create a Bugfix Release for an older version stream

Sometimes it is neccesary or desired to release a bugfix for an older version stream, e.g. if a new major version is started but not yet released it is usually good to still release critical bugfixes or user provided back-ports.

### Prepare the bugfix release
For a bugfix release there are sligly different steps to perform in the prepare phase

- [ ] Create a release record https://projects.eclipse.org/projects/technology.tycho
- [ ] Find the discussion of the release (e.g. https://github.com/eclipse-tycho/tycho/discussions/1401 ) and edit the title to include the new bugfix release, announce your intend there 
```
We plan to do a <bugfixversion> release soon, if there is anything one likes to getting backported please open a PR that targets the <releasebranch> branch.
```
- [ ] Probabbly backport offerd PRs if not already merged and identify possible fixes that should make it into the bugfix release

### Perform the bugfix release

- [ ] Switch to the release branch `tycho-N.M.x` (e.g. `tycho-3.0.x`) 
- [ ] Update version to remove `-SNAPSHOT` to the bugfix version with `mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=<BUGFIX_VERSION>`
- [ ] Review the version changes and commit (but do not push) the changes, e.g. with message `Update versions for release`
- [ ] make sure all tags are fetched with `git fetch --all --tags`
- [ ] Deploy to nexus staging repository with a fresh maven local repository: 
```
mvn clean deploy -Prelease -DskipTests -Dsource=8 -DjdetectJavaApiLink=false \
     -DforgeReleaseId=sonatype-nexus-staging \
     -DforgeReleaseUrl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
     -Dgpg.keyname=<your e-mail of pgpg key> \
     -Dmaven.repo.local=/tmp/tycho-release
```
- [ ] [Publish the staged release](#publish-the-staged-release)
- [ ] `git tag tycho-<BUGFIX_VERSION>`
- [ ] [Publish the sitedoc](#publish-the-sitedoc) for the release
- [ ] Update version to the next bugfix version with `mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=<NEXT_BUGFIX_VERSION>-SNAPSHOT`
- [ ] review, add and commit the changes e.g. with message `Prepare for next version`
- [ ] push the branch
- [ ] push the tag `git push origin tycho-<TYCHO_VERSION>`
- [ ] [Prepare the announcment text](#prepare-the-announcment-text)
- [ ] [Create a release](https://github.com/eclipse-tycho/tycho/releases/new) using the created tag and prepared text
- [ ] Also post this in the release discussion
- [ ] Forward the above text to tycho-dev@eclipse.org
- [ ] Optionally, announce on other medias (Twitter, ...)

## Publish the staged release

- [ ] Inspect the staged content if it looks sane
- [ ] close the staging repository on https://oss.sonatype.org/#stagingRepositories 

![image](https://user-images.githubusercontent.com/1331477/226089500-03236680-7219-4755-8e62-bfe38e5754a3.png)

- [ ] Wait until all checks are done (this takes some time, see Activity tab).
- [ ] If all checks have passed, release the repository.

![image](https://user-images.githubusercontent.com/1331477/226089954-a5a1763c-9497-4b42-afc7-c61e119c637f.png)

- [ ] Wait for artifacts to be available on Maven central, e.g. by looking at https://repo1.maven.org/maven2/org/eclipse/tycho/tycho-core/

## Publish the sitedoc

- [ ] Generate site docs using `mvn install site site:stage -DskipTests -Dmaven.repo.local=/tmp/tycho-release` and check the result from `target/staging` seems viable.
- [ ] checkout (or update) https://github.com/eclipse-tycho/eclipse-tycho.github.io
- [ ] create a new folder for the release and copy the contents of from the `target/staging` folder into this
- [ ] in `docs/index.html` add a new entry for the created folder and check in a brwoser that everything works and seems viable.
- [ ] If applicabale update the symbolic link for the `latest` folder (on windows you must edit the link file with a text editor)
```
eclipse-tycho.github.io/doc$ rm latest
eclipse-tycho.github.io/doc$ ln -s <current release> latest
```
- [ ] add, commit and push your changes

## Prepare the announcment text
 - [ ] Find out who contributed to the release:
```
git log --pretty=format:%an tycho-<previousVersion>..tycho-<newVersion> | sort | uniq
git log --grep="Also-[bB]y:" tycho-<previousVersion>..tycho-<newVersion> | grep -i also-by | sed -e 's/.*Also-[bB]y:\s*\(.*\)/\1/' | sort | uniq
```
   - [ ] Find out who has sponsored something in this release:
   ```
   is:closed is:issue label:sponsored milestone:<version mile stone> 
   ```
   - [ ] Copy and update the follwoing text:
```
Subject: Tycho <VERSION> is released

Tycho <VERSION> has been released and is available from Maven Central repository.

🆕 https://github.com/eclipse-tycho/tycho/blob/tycho-x.y.z/RELEASE_NOTES.md
🏷️ https://github.com/eclipse-tycho/tycho/tree/tycho-x.y.z
👔 https://projects.eclipse.org/projects/technology.tycho/releases/x.y.z
🙏 contributors who contributed patches for this release:
    <contributors>
💰 we would like to also thank <list of sponsors> for sponsoring contributions in this release

and thanks to everyone who helped us with testing the snapshot version.

Regards,

 The Tycho Team

```
