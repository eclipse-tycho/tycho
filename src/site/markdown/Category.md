# Category

A `category.xml` file can be used to define which content is placed into a p2 repository.
It can also specify how to display the content in the p2 installation dialog.
For Tycho to use it, it must to be placed into the root of an project with the packaging type 'eclipse-repository'.

The `category.xml` format was originally defined by the Eclipse PDE project.
There are extensions to the format only supported by p2 and Tycho.

The following snippet is a simple category file listing only one feature and one plug-in.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<site>
   <feature id="com.example.feature">
      <category name="com.example.category.update"/>
   </feature>
   <category-def name="com.example.category.update" label="Example update site"/>
</site>
```

The following is an example, demonstrating a complex category definition.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<site>
   <!-- Include features -->
   <feature id="feature.id" version="1.4.100.v2009"/>
   <!-- Since Tycho 1.1.0 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=407273#c18), features can declare platform-specific compatibility for multi-platform builds -->
   <!-- Examples: https://github.com/mickaelistria/org.eclipse.simrel.build/blob/master/categories/category.xml#L581 -->
   <feature id="linux.specific.feature.id" version="0.0.0" os="linux"/>

   <!-- Directly include bundles, without a feature -->
   <bundle id="bundle.id" version="1.3.1.v2023"/>

   <!-- Directly include any iu -->
   <iu id="unit.id"/>

   <!-- Include all IUs matching an expression -->
   <iu>
     <query>
       <expression type="match">
         <![CDATA[
           id == $0
         ]]>
       </expression>
       <param>another.unit.id</param>
     </query>
   </iu>

   <!-- Categories -->
   <feature id="feature.in.category">
      <category name="category.id"/>
   </feature>
   <category-def name="category.id" label="Category Label">
      <description>Details on the category</description>
   </category-def>

   <!-- example for a dynamic category -->
   <category-def name="javax" label="Bundles starting with javax."/>
   <iu>
      <category name="javax"/>
      <query>
         <expression type="match">id ~= /javax.*/</expression>
      </query>
   </iu>
</site>
```

You can read more about P2 Query Syntax [here](https://wiki.eclipse.org/Equinox/p2/Query_Language_for_p2).

## Managing Update Sites with the P2 Manager

The `tycho-p2-extras:p2-manager` goal provides a way to maintain, update, and manage the integrity of a public update site. This mojo wraps the [P2 Manager application from JustJ Tools](https://eclipse.dev/justj/?page=tools) and offers a more convenient and validated configuration compared to using the eclipse-run goal directly.

The P2 Manager can help with:
- Promoting builds to update sites (nightly, milestone, or release)
- Generating composite repositories
- Managing repository history and retention policies
- Creating browsable HTML pages for your update sites
- Maintaining repository integrity

### Basic Usage

The simplest usage promotes a repository to your update site:

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
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Configuration Parameters

Key parameters include:

- `root` (required): The root folder of the project's update site
- `promote`: Source repository URI to promote
- `type`: Build type - `nightly`, `milestone`, or `release` (default: `nightly`)
- `timestamp`: Build timestamp in format yyyyMMddHHmm
- `retain`: Number of nightly builds to retain (default: 7)
- `label`: The project label to use in generated pages (default: "Project")
- `verbose`: Whether to print progress (default: true)

For a complete list of parameters and advanced options, see the [P2 Manager Mojo documentation](tycho-extras/tycho-p2-extras-plugin/p2-manager-mojo.html).
