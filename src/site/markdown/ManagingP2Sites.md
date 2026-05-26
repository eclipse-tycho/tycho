# Managing P2 Update Sites

Once a p2 repository has been built with Tycho, it typically needs to be published and maintained on a public update site.
The `tycho-p2-extras:p2-manager` goal wraps the [P2 Manager application from JustJ Tools](https://eclipse.dev/justj/?page=tools) to automate this lifecycle.
Compared to invoking the application manually via `eclipse-run`, the mojo provides typed, validated configuration parameters and integrates naturally into the Maven build.

## Update Site Anatomy

The P2 Manager organises a managed update site into a well-defined folder hierarchy based on build type.
Given a `root` of `/var/www/updates/myproject` and a `relative` of `updates`, the resulting structure looks like this:

```
updates/
  nightly/
    latest/          ← composite, always points to the newest nightly
    N202405261200/   ← individual nightly (name = N + timestamp)
    N202405251100/
    ...              ← older nightlies pruned automatically by -retain
  milestone/
    latest/          ← composite, always points to the newest milestone
    S202406010000/   ← individual milestone (name = S + timestamp)
  release/
    latest/          ← composite, always points to the newest release
    1.2.0/           ← release folder named after the logical version
```

Composite repositories are generated automatically at every level, so users can point their Eclipse installation at `updates/nightly/latest` to always track the tip, or at `updates/release/latest` for the latest stable release.

## Build Type Promotion Workflow

The three build types form a deliberate promotion chain:

| Type        | What is promoted                               | Prerequisite                  |
|-------------|------------------------------------------------|-------------------------------|
| `nightly`   | The built repository as a new nightly snapshot | None                          |
| `milestone` | The built repository as a new milestone        | At least one nightly present  |
| `release`   | The **latest milestone** (not the build output)| At least one milestone present|

Note that for a `release` build the `promote` parameter is ignored — the P2 Manager mirrors the latest milestone content byte-for-byte as the release.
This ensures releases are always identical to a validated milestone.

## Basic Usage

Add the mojo to any Maven project (it does not require an `eclipse-repository` packaging type and can run standalone with `requiresProject = false`):

```xml
<plugin>
    <groupId>org.eclipse.tycho.extras</groupId>
    <artifactId>tycho-p2-extras-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
        <execution>
            <id>promote-build</id>
            <goals>
                <goal>p2-manager</goal>
            </goals>
            <configuration>
                <root>${project.build.directory}/updatesite</root>
                <promote>file:${project.build.directory}/repository</promote>
                <timestamp>${maven.build.timestamp}</timestamp>
                <type>nightly</type>
                <label>My Project</label>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Add the timestamp format to your POM properties so the value is in the expected `yyyyMMddHHmm` format:

```xml
<properties>
    <maven.build.timestamp.format>yyyyMMddHHmm</maven.build.timestamp.format>
</properties>
```

## Remote Publishing

For publishing to a remote server the P2 Manager uses `rsync`.
Set `remote` to an rsync-compatible destination and `targetUrl` to the public HTTP(S) URL of the `root` once it has been transferred:

```xml
<configuration>
    <root>${project.build.directory}/updatesite</root>
    <relative>myproject/updates</relative>
    <remote>deploy-user@downloads.example.org:/var/www/downloads</remote>
    <targetUrl>https://downloads.example.org</targetUrl>
    <promote>file:${project.build.directory}/repository</promote>
    <timestamp>${maven.build.timestamp}</timestamp>
    <type>${build.type}</type>
    <label>My Project</label>
    <buildUrl>${env.BUILD_URL}</buildUrl>
    <commit>https://github.com/myorg/myproject/commit/${git.commit}</commit>
    <breadcrumbs>
        <breadcrumb>My Project https://example.org/myproject</breadcrumb>
    </breadcrumbs>
</configuration>
```

When `remote` is not set the P2 Manager only populates `root` locally, which is useful for testing the generated site structure without an actual server.

## Version Determination

The P2 Manager derives the logical version of a repository (used for release folder names and milestone lifecycle management) by inspecting the versions of installable units inside the promoted repository.
Use `versionIU` (prefix match) or `versionIUPattern` (regular expression) to select the relevant IU, typically the project's SDK feature:

```xml
<configuration>
    <versionIU>com.example.sdk</versionIU>
    <!-- or: -->
    <versionIUPattern>com\.example\..*\.sdk</versionIUPattern>
</configuration>
```

Without either setting all IUs are considered, which is usually not what you want.

## Generated HTML Pages

The P2 Manager generates browsable HTML index pages for the update site.
Several parameters control branding:

```xml
<configuration>
    <label>My Project</label>
    <favicon>https://example.org/favicon.ico</favicon>
    <titleImage>https://example.org/logo.png</titleImage>
    <bodyImage>https://example.org/banner.svg</bodyImage>
    <breadcrumbs>
        <breadcrumb>My Project https://example.org</breadcrumb>
        <breadcrumb>Downloads https://example.org/downloads</breadcrumb>
    </breadcrumbs>
    <archives>
        <archive>0.x Archive https://example.org/old-downloads</archive>
    </archives>
</configuration>
```

To show a summary table of installable unit versions across all sites, set `summary` to the number of update site columns to include:

```xml
<configuration>
    <summary>5</summary>
    <!-- optional: restrict which IUs appear in the table -->
    <summaryIUPattern>com\.example\..*(?&lt;!\.source)</summaryIUPattern>
</configuration>
```

## Promoting Products

To promote Tycho-built product archives alongside the p2 repository, set `promoteProducts` to the folder containing the product zip files:

```xml
<configuration>
    <root>${project.build.directory}/updatesite</root>
    <promote>file:${project.build.directory}/repository</promote>
    <promoteProducts>${project.build.directory}/products</promoteProducts>
    <type>${build.type}</type>
</configuration>
```

Download links for each product archive will appear on the generated HTML index page.

## Complete Parameter Reference

| Parameter                  | Default                           | Description                                                              |
|----------------------------|-----------------------------------|--------------------------------------------------------------------------|
| `root`                     | *(required)*                      | Local root folder of the managed update site                             |
| `type`                     | `nightly`                         | Build type: `nightly`, `milestone`, or `release`                         |
| `timestamp`                | current time                      | Build timestamp (`yyyyMMddHHmm`)                                         |
| `promote`                  |                                   | Repository URI or path to promote                                        |
| `relative`                 |                                   | Relative path below `root` for this project's repositories               |
| `remote`                   |                                   | rsync destination for remote publishing                                  |
| `targetUrl`                |                                   | Public base URL of the root once published                               |
| `retain`                   | `7`                               | Number of nightly builds to keep                                         |
| `label`                    | `Project`                         | Project name used in generated HTML pages                                |
| `buildUrl`                 |                                   | CI build URL linked from generated pages                                 |
| `commit`                   |                                   | Git commit URL stored as a repository property                           |
| `versionIU`                |                                   | IU ID prefix for logical version determination                           |
| `versionIUPattern`         |                                   | IU ID regex for logical version determination                            |
| `iuFilterPattern`          |                                   | Regex to restrict which IUs appear in generated index details            |
| `primaryIUFilterPattern`   | `.*\.sdk(…)\.feature\.group`      | Regex selecting primary (SDK) features highlighted on index pages        |
| `excludedCategoriesPattern`|                                   | Regex to remove category IUs from the promoted repository                |
| `baselineUrl`              |                                   | URL to check for baseline artifact replacements                          |
| `promoteProducts`          |                                   | Folder of Tycho product archives to promote alongside the repository     |
| `downloads`                |                                   | Additional file paths to publish as downloads                            |
| `favicon`                  | Eclipse favicon                   | Favicon URL for generated HTML pages                                     |
| `titleImage`               | Eclipse logo                      | Title image URL for generated HTML pages                                 |
| `bodyImage`                |                                   | Body image URL for generated HTML pages                                  |
| `breadcrumbs`              |                                   | Navigation breadcrumbs (`label URL` pairs)                               |
| `archives`                 |                                   | Archive navigation links (`label URL` pairs)                             |
| `mappings`                 |                                   | Name mappings (`raw->Display` pairs) for navigation labels               |
| `commitMappings`           |                                   | Regex mappings to rewrite commit URLs                                    |
| `mavenWrappedMappings`     |                                   | Mappings to rewrite or remove `maven-wrapped-` IU properties             |
| `excludes`                 |                                   | File names passed as `--exclude` to rsync                                |
| `summary`                  | `0` (disabled)                    | Number of update site columns in the IU summary table                    |
| `summaryIUPattern`         | excludes sources and features     | Regex selecting IUs shown in the summary table                           |
| `simrelAlias`              | `false`                           | Create a SimRel-named alias (e.g. `2024-06`) for the current version     |
| `bree`                     | `false`                           | Generate minimum execution environment details per bundle                |
| `verbose`                  | `true`                            | Print detailed progress (set to `false` for quiet output)                |
| `managerRepository`        | JustJ tools update site           | Maven repository from which the P2 Manager application is resolved       |

For the full Mojo reference, see the [P2 Manager Mojo Reference](tycho-extras/tycho-p2-extras-plugin/p2-manager-mojo.html).

## Further Reading

- [Building P2 Update Sites](BuildingSites.html) — How to build a p2 repository with Tycho
- [JustJ P2 Manager documentation](https://eclipse.dev/justj/?page=tools) — Upstream documentation for the wrapped application
- [P2 Manager Mojo Reference](tycho-extras/tycho-p2-extras-plugin/p2-manager-mojo.html) — Auto-generated parameter reference
