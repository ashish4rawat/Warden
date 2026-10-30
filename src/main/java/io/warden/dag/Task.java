package io.warden.dag;

import java.util.Set;

/**
 * A single step in a pipeline.
 * Just an id and what it depends on for now. Inputs/outputs and contracts come in v0.2
 */
public record Task(String id, Set<String> dependsOn) {

    public Task {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("task id can't be empty");
        }
        dependsOn = dependsOn == null ? Set.of() : Set.copyOf(dependsOn);
    }
}
