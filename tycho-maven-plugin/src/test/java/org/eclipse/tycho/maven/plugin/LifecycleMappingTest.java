package org.eclipse.tycho.maven.plugin;

import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecycleMojo;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.eclipse.sisu.launch.Main;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Just a small little test to make sure all mappings are here.
 */
@Named
public class LifecycleMappingTest {
    @Inject
    private Map<String, LifecycleMapping> lifecycleMappings;

    @Test
    void smoke() {
        LifecycleMappingTest self = Main.boot(LifecycleMappingTest.class);
        assertEquals(7, self.lifecycleMappings.size());
        System.out.println("All mappings defined in this plugin:");
        for (Map.Entry<String, LifecycleMapping> mapping : self.lifecycleMappings.entrySet()) {
            System.out.println("* " + mapping.getKey());
            for (Map.Entry<String, LifecyclePhase> phases : mapping.getValue().getLifecycles().get("default").getLifecyclePhases().entrySet()) {
                System.out.println("  " + phases.getKey());
                for (LifecycleMojo mojo : phases.getValue().getMojos()) {
                    System.out.println("   -> " + mojo.getGoal());
                }
            }
        }
    }
}
