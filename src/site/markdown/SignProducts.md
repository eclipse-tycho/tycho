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
