package org.eclipse.tycho.sbom;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.cyclonedx.maven.ModelConverter;
import org.cyclonedx.maven.ProjectDependenciesConverter;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * The problem is not component definition (it is JSR330), but component injection point. It is happening in a Mojo,
 * that uses third annotation (plugin component annotations {@code @Component}) that is nor a Plexus annotation nor is
 * JSR330 annotation. The trace to injection existing ONLY in plugin.xml (that is EXTENDING component.xml), and
 * is manually pushed into Plexus container. So all that Plexus can do (as it really has only descriptor, field is
 * without any hint or annotation) is to blindly follow Plexus XML.
 *
 * Hence, we need to <em>bind these components exactly the same way as Plexus Shim would do</em>. This is why
 * we do this "dance" with Guice binds.
 *
 * If CycloneDX project would abstain of using Maven Plugin API {@code @Component}, but would use standard {@link @Inject},
 * none of this would be needed.
 */
@Named
public class TychoSBOMOverrides extends AbstractModule {
    @Override
    protected void configure() {
        bind(Key.get(TypeLiteral.get(ModelConverter.class), Names.named("default"))).to(TychoModelConverter.class).in(Singleton.class);
        bind(Key.get(TypeLiteral.get(ProjectDependenciesConverter.class), Names.named("default"))).to(TychoProjectDependenciesConverter.class);
    }
}
