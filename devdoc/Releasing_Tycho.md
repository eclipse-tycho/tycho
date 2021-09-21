# Releasing Tycho

This describes the steps to perform a release of Tycho:

- [ ] Make sure all fixed issues and merged PRs have the correct milestone for this release, to finding PRs without milestone you can use the following filter `is:pr is:merged no:milestone` isuues without milestone can be found with `is:issue no:milestone is:closed` 
- [ ] Prepare the [release notes](https://github.com/eclipse/tycho/blob/master/RELEASE_NOTES.md) which should provide a quick overview of new features and bug fixes 
- [ ] Create branch `tycho-N.M.x` (eg `tycho-2.4.x`) for upcoming release and push it to remote; this branch should remain frozen until the release, only major fixes for regressions could be merged in before release. Work can still happen regularly on the `master` branch.
- [ ] Create release record on https://projects.eclipse.org/projects/technology.tycho projects.eclipse.org , link the N&N to https://github.com/eclipse/tycho/blob/[branch-name]/RELEASE_NOTES.md release notes]
- [ ] Update versions on `master `to future release with `mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=<NEXT_VERSION>-SNAPSHOT` and push to remote 
- [ ] Announce the intent to release and request feedback about snapshots on tycho-user@eclipse.org and [GitHub discussions](https://github.com/eclipse/tycho/discussions):
```
Subject: Please test snapshots for upcoming <VERSION> release

We plan to release Tycho <VERSION> next week. For details of new features and bugfixes, see release notes [1].
Please help by testing the SNAPSHOTS build. To use it, change your tycho version to <VERSION>-SNAPSHOT and add snippet [2] to your pom.

We plan to promote this release in one week unless major regressions are found.

Regards,
Tycho team

[1] https://github.com/eclipse/wildwebdeveloper/blob/master/RELEASE_NOTES.md
[2]
<pluginRepositories>
    <pluginRepository>
      <id>tycho-snapshots</id>
      <url>https://repo.eclipse.org/content/repositories/tycho-snapshots/</url>
    </pluginRepository>
</pluginRepositories>
```

... Wait until review date (usually a week later)...

- [ ] Make sure you have everything setup (GPG installed!) for deploying to the Nexus OSS repository, see https://central.sonatype.org/pages/ossrh-guide.html guide
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

- [ ] Checkout branch `tycho-N.M.x`
- [ ] Update version to remove `-SNAPSHOT` with `mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=<VERSION>`
- [ ] Update versions in tycho-demo folder
- [ ] Sync to release commit and deploy to nexus staging repository: `mvn clean deploy -Prelease -DforgeReleaseId=sonatype-nexus-staging -DforgeReleaseUrl=https://oss.sonatype.org/service/local/staging/deploy/maven2/`
- [ ] On https://oss.sonatype.org/#stagingRepositories , Close the staging repository, get the staging repo URL from Nexus
- [ ] Wait for artifacts to be available on Maven central.
- `git tag <TYCHO_VERSION>`
- push tag to remote
- Documentation
  - [ ] Generate site docs using `mvn install site site:stage -DskipTests` and check the result from `target/staging` seems viable.
  - [ ] Prepare documentation on the webite, using git repo https://git.eclipse.org/c/www.eclipse.org/tycho.git/ : copy the local site docs folders `target/staging/sitedocs` to the existing `sitedocs` folder and then submit as Gerrit review (don't merge yet)
- Announce the release
   - [ ] Find out who contributed to the release:
```
git log --pretty=format:%an tycho-<previousVersion>..tycho-<newVersion> | sort | uniq
git log --grep="Also-[bB]y:" tycho-<previousVersion>..tycho-<newVersion> | grep -i also-by | sed -e 's/.*Also-[bB]y:\s*\(.*\)/\1/' | sort | uniq
```
  - [ ] Announce the release on [GitHub discussions](https://github.com/eclipse/tycho/discussions) and tycho-dev@eclipse.org, thanking the contributors:
```
Subject: Tycho <VERSION> is released

Tycho <VERSION> has been released and is available from Maven Central repository.

🆕 https://github.com/eclipse/tycho/blob/master/RELEASE_NOTES.md#xyz
🏷️ https://github.com/eclipse/tycho/tree/tycho-x.y.z
👔 https://projects.eclipse.org/projects/technology.tycho/releases/x.y.z
🙏 contributors who contributed patches for this release:
<contributors>
and thanks and to everyone who helped us with testing the staged version.

Regards,

```
   - [ ] Optionally, announce on other medias (Twitter)
- [ ] Create, push and merge patch changing the bootstrap Tycho version for Tycho build (eg https://git.eclipse.org/r/c/tycho/org.eclipse.tycho/+/174964 )
