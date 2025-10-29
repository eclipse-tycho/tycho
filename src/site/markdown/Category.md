# Category

A `category.xml` file can be used to define which content is placed into a p2 repository.
It can also specify how to display the content in the p2 installation dialog.
For Tycho to use it, it must to be placed into the root of an project with the packaging type 'eclipse-repository'.

**Note:** The `eclipse-repository` packaging type can also be used to build Eclipse products (RCP applications). See [Building Products](Products.html) for more information on product builds.

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
