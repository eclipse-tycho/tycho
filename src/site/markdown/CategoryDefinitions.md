# Advanced Category Definitions

The `category.xml` file supports powerful features beyond simple feature and bundle listings.
Using p2 query expressions, you can dynamically select installable units and organize them into categories based on patterns, properties, and other criteria.

## Dynamic Categories with Query Expressions

Instead of listing each installable unit (IU) individually, you can use `<query>` elements to match IUs dynamically.
This is especially useful when the set of IUs changes over time or when you want to organize large numbers of IUs automatically.

### Basic Structure

A query is placed inside an `<iu>` element. The query uses an `<expression>` with `type="match"` to define the matching criteria:

```xml
<iu>
  <query>
    <expression type="match">
      <!-- p2 match expression -->
    </expression>
  </query>
</iu>
```

The expression is evaluated against every installable unit that is available in the build context.
Each IU that matches the expression is included in the repository.

### Assigning Queried IUs to Categories

To assign all matched IUs to a category, add a `<category>` element inside the `<iu>`:

```xml
<category-def name="javax" label="Javax Bundles"/>
<iu>
   <category name="javax"/>
   <query>
      <expression type="match">id ~= /javax.*/</expression>
   </query>
</iu>
```

This creates a category called "Javax Bundles" and places all IUs whose ID starts with `javax.` into it.

## P2 Query Expression Syntax

P2 query expressions operate on the properties of installable units. The expression language supports comparison operators, logical operators, pattern matching, and access to IU metadata.

### IU Properties Available in Expressions

Each installable unit exposes the following properties that can be used in match expressions:

| Property | Type | Description |
|----------|------|-------------|
| `id` | String | The identifier of the installable unit |
| `version` | Version | The version of the installable unit |
| `properties` | Map | Key-value properties of the IU (e.g., `properties['org.eclipse.equinox.p2.name']`) |
| `providedCapabilities` | Collection | Capabilities provided by the IU |
| `requirements` | Collection | Requirements (dependencies) of the IU |
| `singleton` | Boolean | Whether the IU is a singleton |

### Comparison Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `==` | Equal to | `id == 'com.example.bundle'` |
| `!=` | Not equal to | `id != 'com.example.internal'` |
| `<` | Less than (for versions) | `version < '2.0.0'` |
| `<=` | Less than or equal | `version <= '2.0.0'` |
| `>` | Greater than | `version > '1.0.0'` |
| `>=` | Greater than or equal | `version >= '1.0.0'` |

### Logical Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `&&` | Logical AND | `id ~= /com.example.*/ && version >= '1.0.0'` |
| <code>&#124;&#124;</code> | Logical OR | `id == 'bundle.a' \|\| id == 'bundle.b'` |
| `!` | Logical NOT | `!(id ~= /.*\.source/)` |

### Pattern Matching

| Operator | Description | Example |
|----------|-------------|---------|
| `~=` | Matches a regular expression | `id ~= /com\.example\..*/` |

Regular expressions are delimited by `/` characters and follow standard regex syntax.

### Parameter Substitution

Expressions can reference parameters defined in `<param>` elements using `$0`, `$1`, etc.:

```xml
<iu>
  <query>
    <expression type="match">
      <![CDATA[id == $0]]>
    </expression>
    <param>com.example.specific.bundle</param>
  </query>
</iu>
```

This is equivalent to `<iu id="com.example.specific.bundle"/>` but enables more complex parameterized patterns.

### Working with Capabilities

You can query IU provided capabilities using the `providedCapabilities` property.
Each capability has a `namespace`, `name`, and `version`:

```xml
<iu>
  <query>
    <expression type="match">
      <![CDATA[
        providedCapabilities.exists(cap | cap.namespace == 'osgi.bundle' && cap.name ~= /com\.example.*/)
      ]]>
    </expression>
  </query>
</iu>
```

This matches any IU that provides an OSGi bundle capability with a name starting with `com.example`.

### Working with Properties

IU properties can be accessed via the `properties` map:

```xml
<iu>
  <query>
    <expression type="match">
      <![CDATA[
        properties['org.eclipse.equinox.p2.type.category'] == 'true'
      ]]>
    </expression>
  </query>
</iu>
```

## Practical Examples

### Group All Bundles by Prefix

Automatically categorize bundles from different vendors:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<site>
   <!-- Category definitions -->
   <category-def name="example" label="Example Project Bundles"/>
   <category-def name="thirdparty" label="Third-Party Dependencies"/>

   <!-- All com.example.* IUs go into the "example" category -->
   <iu>
      <category name="example"/>
      <query>
         <expression type="match">id ~= /com\.example\..*/</expression>
      </query>
   </iu>

   <!-- All org.apache.* IUs go into the "thirdparty" category -->
   <iu>
      <category name="thirdparty"/>
      <query>
         <expression type="match">id ~= /org\.apache\..*/</expression>
      </query>
   </iu>
</site>
```

### Include Everything Except Source Bundles

```xml
<iu>
  <query>
    <expression type="match">
      <![CDATA[
        !(id ~= /.*\.source/)
      ]]>
    </expression>
  </query>
</iu>
```

### Combine Multiple Criteria

Match IUs that belong to a specific namespace and have a minimum version:

```xml
<iu>
  <query>
    <expression type="match">
      <![CDATA[
        id ~= /com\.example\..*/ && version >= '2.0.0'
      ]]>
    </expression>
  </query>
</iu>
```

### Include Specific IU by Parameterized Query

When using CDATA sections (required when the expression contains special XML characters like `<`, `>`, or `&`), parameter substitution keeps expressions clean:

```xml
<iu>
  <query>
    <expression type="match">
      <![CDATA[id == $0]]>
    </expression>
    <param>jakarta.annotation-api</param>
  </query>
</iu>
```

## Tips

- Use `<![CDATA[ ... ]]>` to wrap expressions that contain XML special characters (`<`, `>`, `&`).
- The `type="match"` attribute on the `<expression>` element is required. Only `match` expressions are supported in Tycho category definitions.
- Queries are evaluated against all IUs available in the target platform, not just those built by the current reactor. Use `filterProvided` on the repository plugin to exclude IUs from referenced repositories if needed (see [Controlling Repository Content](RepositoryContent.html)).
- For simple cases where you just need to include a known IU, prefer `<iu id="..."/>` or `<bundle id="..."/>` over query expressions.
