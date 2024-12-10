# Overarch Java
Overarch Java provides an annotation processor for the java compiler to generate Overarch models from java sources.

*draft*

[![Clojars Project](https://img.shields.io/clojars/v/org.soulspace/overarch-java.svg)](https://clojars.org/org.soulspace/overarch-java)
![GitHub](https://img.shields.io/github/license/soulspace-org/overarch-java)

## Build
Currently [leiningen](https://leiningen.org) is used to build the `overarch-java.jar`.

For a local build and installation in the maven repository use
```
lein install
```

## Usage
Add the clojars repo to your build, e.g for maven by adding this repository to the repositories in your `pom.xml`.
```xml
<repository>
  <id>clojars.org</id>
  <url>https://repo.clojars.org</url>
</repository>
```

Add the `overarch-java` and `overarch-java-annotations` dependencies to your project
and annotate your java sources with the overarch annotations.

On compilation, the annotation processor processes the annotated source files
and generates a `model.edn` file containing the set of model elements derived
from the sources.

## Example
Create a component model element by annotating a package in the `package-info.java` file:

```java
/**
 * The User Management component provides the user related functionality.
 */
@OverarchNode(el = "component", id = "example/user", name = "User Management")
package example.user;

import org.soulspace.overarch.java.OverarchNode;
```

Create a component model element by annotating a class:

```java
package example.user.application;

import java.util.UUID;

import org.soulspace.overarch.java.OverarchNode;

import example.user.domain.User;

/**
 * Implements the use cases of the user management component
 */
@OverarchNode(el = "class" , id = "example.user.application/user-service")
public class UserService {
    public User createUser(String firstname, String lastName, String email) {
        // TODO implement
        return null;
    };

    public boolean deleteUser(UUID id) {
        // TODO implement
        return false;
    };
}
```

## Copyright
©  2024 Ludger Solbach

## License
Eclipse Public License 1.0 (EPL1.0)
