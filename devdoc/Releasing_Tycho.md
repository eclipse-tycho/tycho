This page describes the steps necessary to create releases of the Tycho project. 

- [ ] Make sure all fixed issues and merged PRs have the correct milestone for this release
- [ ] Prepare the [release notes](https://github.com/eclipse/tycho/blob/master/RELEASE_NOTES.md) which should provide a quick overview of new features and bug fixes 
- [ ] Create release record on https://projects.eclipse.org/projects/technology.tycho projects.eclipse.org , link the N&N to https://github.com/eclipse/tycho/blob/master/RELEASE_NOTES.md release notes]
- [ ] Update versions using `mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=<VERSION>` 
- [ ] Commit version change, and create a git tag `tycho-<VERSION>` on this commit  
- [ ] Update versions (same as above) to next `-SNAPSHOT` development version and push commit to `master` branch 
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
- [ ] Checkout the tag version `tycho-<VERSION>` so current commit is the tag.
- [ ] Sync to release commit and deploy to nexus staging repository: `mvn clean deploy -Prelease -DforgeReleaseId=sonatype-nexus-staging -DforgeReleaseUrl=https://oss.sonatype.org/service/local/staging/deploy/maven2/`
- [ ] In parallel of previous <tt>mvn</tt> command, prepare update [tycho-demo projects](http://git.eclipse.org/c/tycho/org.eclipse.tycho-demo.git) to the latest release and push the change as a review to Gerrit
- [ ] When previous `mvn` command is complete, generate site docs using `mvn install site site:stage -DskipTests` and check the result from `target/staging` seems viable.
- [ ] On https://oss.sonatype.org/#stagingRepositories , Close the staging repository, get the staging repo URL from Nexus
- [ ] Update [release notes](https://github.com/eclipse/tycho/blob/master/RELEASE_NOTES.md) to replace reference to snapshot build to reference to staging repo
- [ ] Announce the temporary stage URL on tycho-user@eclipse.org:
```
Subject: please test staged tycho <VERSION>

Tycho release <VERSION&> has been staged. For details of new features and bugfixes, see release notes [1].
Please help by testing the staged build. To use it, change your tycho version to <VERSION> and add snippet [2] to your pom.

We plan to promote this release in one week unless major regressions are found.

Regards,
Tycho team

[1] https://github.com/eclipse/wildwebdeveloper/blob/master/RELEASE_NOTES.md
[2]
  <pluginRepositories>
    <pluginRepository>
      <id>tycho-staged</id>
      <url><NEXUS_OSS_STAGING_URL></url
    </pluginRepository>
  </pluginRepositories>
</pre>
```
- [ ] Prepare documentation on  the webite, using git repo https://git.eclipse.org/c/www.eclipse.org/tycho.git/ : copy the local site docs folders `target/staging/sitedocs` to the existing `sitedocs` folder and then submit as Gerrit review (don't merge yet)

... Wait until review date (usually a week later)...

- [ ] After ~1 week of testing, promote the stage repository on https://oss.sonatype.org/
- [ ] Wait for artifacts to be available on Maven central.
- [ ] Push the release tags to git: `git push origin --tags`
- [ ] Update https://github.com/eclipse/tycho/blob/master/RELEASE_NOTES.md to remove reference to staging repo
- [ ] Merge the update to `org.eclipse.tycho-demo` and website in Gerrit
- [ ] Announce the release
   - [ ] Find out who contributed to the release:
```
git log --pretty=format:%an tycho-<previousVersion>..tycho-<newVersion> | sort | uniq
git log --grep="Also-[bB]y:" tycho-<previousVersion>..tycho-<newVersion> | grep -i also-by | sed -e 's/.*Also-[bB]y:\s*\(.*\)/\1/' | sort | uniq
```
  - [ ] Announce the release on tycho-user@eclipse.org, tycho-dev@eclipse.org, thanking the contributors:
```
Subject: Tycho <VERSION> released

Tycho <VERSION> has been released and is available from Maven Central repository.

See the release notes 
[1] https://github.com/eclipse/tycho/blob/master/RELEASE_NOTES.md#n for details of enhancements and bug fixes in this release.

Thanks to 
&lt;contributors&gt;
who contributed patches for this release, and thanks and to everyone who helped us with testing the staged version.

Regards,

```
   - [ ] Optionally, announce on other medias (Twitter)
- [ ] Create, push and merge patch changing the bootstrap Tycho version for Tycho build (eg https://git.eclipse.org/r/c/tycho/org.eclipse.tycho/+/174964 )


