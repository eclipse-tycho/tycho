# Signing of custom Eclipse Products with Tycho

In some cases one wants to customize the Eclipse Launcher for example with an own icon what breaks the signing under windows as the icons are part of the executable itself,
also sometimes one wants to sign it with an own company certificate.

As the process of signing an executable can vary and there is no platform independent way to do this Tycho can not do it on its own and the process to do it properly requires some steps,
so we try to give some guidance here.

**Important note** Signing the binary does not really sign your product as a whole! As Eclipse is extensible by nature it can be modified or the binary can be copied to a complete different product!

As signing is usually not performed during normal development, it might be good to put the following configurations into a `profile` e.g. with the name sign-products, the current process is quite convoluted so we likely want to improve that over time to make it simpler.

## Step 1: Move some of the executions to the compile phase

Because we want to modify some files before they appear in the assembly repository, we first move some of the executions from the default phase into the compile phase to execute additional actions:

```xml
<plugin>
	<groupId>org.eclipse.tycho</groupId>
	<artifactId>tycho-p2-publisher-plugin</artifactId>
	<version>${tycho-version}</version>
	<executions>
		<execution>
			<id>default-publish-osgi-ee</id>
			<phase>compile</phase>
		</execution>
		<execution>
			<id>default-publish-products</id>
			<phase>compile</phase>
		</execution>
		<execution>
			<id>default-attach-artifacts</id>
			<phase>compile</phase>
		</execution>
	</executions>
</plugin>
```

## Step 2: Expand the customized product binaries

Maven and P2 manage artifacts as zip files so they can be transferred as a stream, because usually the signing tools are not capable to sign binaries inside a zip file we first need to extract them we use the windows binaries here as an example:

```xml
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-dependency-plugin</artifactId>
	<version>3.9.0</version>
	<executions>
		<execution>
			<id>unpack</id>
			<phase>process-classes</phase>
			<goals>
				<goal>unpack</goal>
			</goals>
			<configuration>
				<outputDirectory>${project.build.directory}/executables</outputDirectory>
				<artifactItems>
					<artifactItem>
						<groupId>${project.groupId}</groupId>
						<artifactId>${project.artifactId}</artifactId>
						<classifier><!-- ID of your custom product -->.executable.win32.win32.x86_64</classifier>
						<version>${project.version}</version>
						<type>zip</type>
					</artifactItem>
				</artifactItems>
			</configuration>
		</execution>
	</executions>
</plugin>
```

## Step 3: perform the signing (or other customization)

Now we need to sign the binaries, as explained this a custom step that depends on how you do the actual signing, as an example we here instead delete the eclipsec.exe instead of performing any signing:

```xml
<plugin>
	<artifactId>maven-clean-plugin</artifactId>
	<version>2.5</version>
	<executions>
		<execution>
			<id>delete</id>
			<phase>process-classes</phase>
			<goals>
				<goal>clean</goal>
			</goals>
			<configuration>
				<excludeDefaultDirectories>true</excludeDefaultDirectories>
				<filesets>
					<fileset>
						<directory>${project.build.directory}/executables</directory>
						<includes>
							<include>eclipsec.exe</include>
						</includes>
					</fileset>
				</filesets>
			</configuration>
		</execution>
	</executions>
</plugin>
```

## Step 4: Make a zip of the results

Now we need to package them as zip files again and place them at the location where Tycho is looking for them:

```xml
<plugin>
	<groupId>org.eclipse.tycho</groupId>
	<artifactId>tycho-p2-repository-plugin</artifactId>
	<version>${tycho-version}</version>
	<executions>
		<execution>
			<id>pack</id>
			<phase>process-classes</phase>
			<goals>
				<goal>archive-repository</goal>
			</goals>
			<configuration>
				<finalName>extraArtifacts/<!-- ID of your custom product -->.executable.win32.win32.x86_64</finalName>
				<repositoryLocation>${project.build.directory}/executables</repositoryLocation>
			</configuration>
		</execution>
	</executions>
</plugin>
```

## Result

You will now end up with a product for windows that only have `eclipse.exe` (or whatever your launcher name was) without the corresponding `eclipsec.exe`, you can find the example [here](https://github.com/eclipse-tycho/tycho/tree/master/demo/custom-signing-product).

## Implementation Details

This section explains where in Tycho and Eclipse P2 the product binary zip files are created and how they flow through the build system. Understanding this implementation can help when troubleshooting or customizing the product signing process.

### Overview of the Product Publishing Flow

The product publishing process in Tycho involves several components working together:

1. **PublishProductMojo** - The entry point Maven mojo
2. **PublishProductToolImpl** - Tycho's implementation of product publishing
3. **Eclipse P2 ProductAction** - P2's publisher action for products
4. **ModuleArtifactRepository** - Tycho's artifact repository implementation

### Detailed Code Flow

#### 1. Entry Point: PublishProductMojo

**Location:** `tycho-p2-publisher-plugin/src/main/java/org/eclipse/tycho/plugins/p2/publisher/PublishProductMojo.java`

This is the `publish-products` mojo that processes all `.product` files in your project. Key responsibilities:

- Reads product definition files from the project directory
- Validates product configuration (id, version, launcher settings)
- Obtains the Equinox executable feature (launcher binaries) from dependencies
- Delegates to `PublishProductToolImpl` for actual publishing
- Calculates SHA-256 checksums for published binary artifacts

The mojo extracts the launcher binaries from `org.eclipse.equinox.executable` feature if the product includes launchers (`includeLaunchers()` returns true). These binaries are passed to the publishing tool.

#### 2. Product Publishing: PublishProductToolImpl

**Location:** `tycho-core/src/main/java/org/eclipse/tycho/p2/tools/publisher/PublishProductToolImpl.java`

This class bridges Tycho and Eclipse P2's publisher infrastructure:

- Creates a P2 `ProductAction` with the expanded product definition
- Configures the artifact repository for writing with `ProductBinariesWriteSession`
- Executes the P2 ProductAction via `PublisherActionRunner`
- Returns dependency seeds for the published product IU

The `ProductBinariesWriteSession` is crucial - it determines the Maven classifier and file extension for binary artifacts (typically `.zip`).

#### 3. P2 ProductAction and ApplicationLauncherAction

**Location (Eclipse P2 repo):** `org.eclipse.equinox.p2.publisher.eclipse/ProductAction.java` and `ApplicationLauncherAction.java`

The P2 `ProductAction` orchestrates several sub-actions:
- `ApplicationLauncherAction` - Publishes executable launcher files
- `ConfigCUsAction` - Creates configuration units
- `RootIUAction` - Creates the root installable unit
- `JREAction` - Handles JRE inclusion if configured

The `ApplicationLauncherAction` creates `EquinoxExecutableAction` instances for each target platform configuration (Windows, Linux, macOS, etc.).

#### 4. Binary Artifact Creation: EquinoxExecutableAction

**Location (Eclipse P2 repo):** `org.eclipse.equinox.p2.publisher.eclipse/EquinoxExecutableAction.java`

This is where the binary executable artifacts are actually created:

1. **Branding**: Applies product branding to executables (icons, names, etc.)
2. **Artifact Key Creation**: Creates a binary artifact key using `PublisherHelper.createBinaryArtifactKey()`
3. **Zip Creation**: The `publishArtifact()` method (from `AbstractPublisherAction`) creates a temporary zip file containing all launcher files using `FileUtils.zip()`
4. **Publishing**: The zip is written to the artifact repository via `destination.getOutputStream(descriptor)`

The classifier for these artifacts follows the pattern: `<product-id>.executable.<ws>.<os>.<arch>` (e.g., `myproduct.executable.win32.win32.x86_64`).

#### 5. Artifact Storage: ModuleArtifactRepository

**Location:** `tycho-core/src/main/java/org/eclipse/tycho/p2/repository/module/ModuleArtifactRepository.java`

This Tycho component manages the module's artifact repository:

- Stores artifacts in the build output directory (`target/`)
- Maintains two metadata files:
  - `p2artifacts.xml` - P2 artifact metadata with Maven coordinates
  - `local-artifacts.properties` - Maps classifiers to file locations
- Uses `ProductBinariesWriteSession` to determine artifact classifier and extension
- Artifacts are stored as zip files that can be attached to the Maven project

When P2 calls `getOutputStream()` on the repository, the repository:
1. Creates an `IArtifactSink` for the new artifact
2. Determines the file location based on Maven coordinates (GAV + classifier)
3. Returns an output stream that writes to the determined location
4. Commits the artifact metadata to `p2artifacts.xml` when the stream closes

#### 6. Artifact Attachment: AttachPublishedArtifactsMojo

**Location:** `tycho-p2-publisher-plugin/src/main/java/org/eclipse/tycho/plugins/p2/publisher/persistence/AttachPublishedArtifactsMojo.java`

The `attach-artifacts` mojo attaches all published artifacts to the Maven project:
- Retrieves artifact locations from the publishing repository
- Attaches each artifact with its classifier and type
- Main artifact gets no classifier, additional artifacts (like platform-specific executables) get classifiers

These attached artifacts are then available for:
- Installation into the local Maven repository
- Deployment to remote Maven repositories
- Extraction and customization (as shown in the signing steps above)

### Key Classes Reference

#### Tycho Classes
- **PublishProductMojo**: Entry point for product publishing
- **PublishProductToolImpl**: Tycho's product publishing implementation
- **ProductBinariesWriteSession**: Determines classifier/extension for binary artifacts
- **ModuleArtifactRepository**: Stores artifacts in module's build directory
- **PublisherActionRunner**: Executes P2 publisher actions
- **AttachPublishedArtifactsMojo**: Attaches artifacts to Maven project

#### Eclipse P2 Classes (External Dependency)
- **ProductAction**: Orchestrates product publishing
- **ApplicationLauncherAction**: Publishes launcher artifacts
- **EquinoxExecutableAction**: Creates and publishes executable binary artifacts
- **AbstractPublisherAction.publishArtifact()**: Creates zip files from executable files
- **PublisherHelper**: Utility for creating artifact keys and descriptors

### Why Artifacts Are Zip Files

The executable binaries are stored as zip files for several reasons:

1. **Multiple Files**: Each platform's launcher includes multiple files (executables, shared libraries, configuration files)
2. **P2 Artifact Model**: P2's artifact repository model uses a single file per artifact
3. **Maven Repository Compatibility**: Maven repositories work with single files per artifact
4. **Streaming**: Zip format allows efficient streaming during repository operations

This is why Step 2 in the signing process needs to extract the zip file - the actual executables are packaged inside.

### Extension Points for Customization

The signing workflow shown above exploits several extension points:

1. **Phase Reordering**: Moving publish-products to `compile` phase allows interception before `package`
2. **Maven Classifiers**: Each platform's executable has a unique classifier for targeted extraction
3. **File System Access**: Extracted files are in standard build directory for tool access
4. **Repository Plugin**: `tycho-p2-repository-plugin`'s `archive-repository` goal can repackage customized files

### Notes on P2 Integration

Some types in Tycho extend or wrap P2 types:

- **ModuleArtifactRepository** extends P2's `ArtifactRepositoryBaseImpl`
- **ProductConfiguration** wraps P2's `ProductFile`
- **PublisherActionRunner** wraps P2's `Publisher`

This allows Tycho to integrate Maven concepts (GAV coordinates, classifiers) with P2's metadata model while leveraging P2's publisher infrastructure for the actual product generation.
