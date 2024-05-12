# Repositories

Repositories (also knows as P2 Updatesites) contain artifacts and metadata to install content into eclipse or use them in a Tycho build.

## Create Repositories using category.xml

A category.xml file can be used to define which content is placed into a p2 repository. 
It can also specify how to display the content in the p2 installation dialog. 
For Tycho to use it, it must to be placed into the root of an project with the packaging type 'eclipse-repository'.

The 'category.xml' format was originally defined by the Eclipse PDE project. 
There are extensions to the format only supported by p2 and Tycho.

The following listing is a simple category file listing only one feature and one plug-in.


```
<?xml version="1.0" encoding="UTF-8"?>
<site>
   <feature id="com.example.feature">
      <category name="com.example.category.update"/>
   </feature>
   <category-def name="com.example.category.update" label="Example update site"/>
</site>
```
The following is an example, demonstrating a complex category definition.

```
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

## Managing Repositories

Tycho offers some tools to manage existing repositories as a replacement for the ant-tasks described [here](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/guide/p2_repositorytasks.htm)

### repo2runnable

See [tycho-p2-repository:repo-to-runnable](tycho-p2-repository-plugin/repo-to-runnable-mojo.html)

### remove.iu

See [tycho-p2-repository:remove-iu](tycho-p2-repository-plugin/remove-iu-mojo.html)
