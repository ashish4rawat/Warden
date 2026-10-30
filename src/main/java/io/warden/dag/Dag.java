package io.warden.dag;

import java.util.*;

/**
 * A pipeline = a set of tasks + the edges between them.
 *
 * Usage:
 * <pre>
 * Dag dag = Dag.builder("daily_sales")
 *         .task("extract")
 *         .task("clean", "extract")
 *         .task("load", "clean")
 *         .build();
 * </pre>
 *
 * build() fails if a dependency doesn't exist or if there's a cycle,
 * so a broken dag never makes it to the scheduler.
 */
public final class Dag {

    private final String id;
    private final Map<String, Task> tasks;

    private Dag(String id, Map<String, Task> tasks) {
        this.id = id;
        this.tasks = Collections.unmodifiableMap(tasks);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() {
        return id;
    }

    public Collection<Task> tasks() {
        return tasks.values();
    }

    public Optional<Task> task(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    /** tasks that nothing depends on upstream, i.e. where a run starts */
    public List<Task> roots() {
        return tasks.values().stream()
                .filter(t -> t.dependsOn().isEmpty())
                .toList();
    }

    /** tasks that directly depend on the given task */
    public List<Task> downstreamOf(String taskId) {
        return tasks.values().stream()
                .filter(t -> t.dependsOn().contains(taskId))
                .toList();
    }

    /**
     * Order to run tasks in. Kahn's algorithm.
     * Throws if there's a cycle (shouldn't happen after build(), but keeping it safe)
     */
    public List<Task> topologicalOrder() {
        Map<String, Integer> inDegree = new HashMap<>();
        for (Task t : tasks.values()) {
            inDegree.put(t.id(), t.dependsOn().size());
        }

        Deque<String> ready = new ArrayDeque<>();
        inDegree.forEach((taskId, deg) -> {
            if (deg == 0) ready.add(taskId);
        });

        List<Task> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String current = ready.poll();
            order.add(tasks.get(current));

            for (Task next : downstreamOf(current)) {
                int remaining = inDegree.merge(next.id(), -1, Integer::sum);
                if (remaining == 0) ready.add(next.id());
            }
        }

        if (order.size() != tasks.size()) {
            throw new InvalidDagException("dag '" + id + "' has a cycle");
        }
        return order;
    }

    public static final class Builder {
        private final String id;
        // linked so tasks keep the order they were added in
        private final Map<String, Task> tasks = new LinkedHashMap<>();

        private Builder(String id) {
            this.id = id;
        }

        public Builder task(String taskId, String... dependsOn) {
            if (tasks.containsKey(taskId)) {
                throw new InvalidDagException("duplicate task '" + taskId + "' in dag '" + id + "'");
            }
            tasks.put(taskId, new Task(taskId, Set.copyOf(List.of(dependsOn))));
            return this;
        }

        public Dag build() {
            if (id == null || id.isBlank()) {
                throw new InvalidDagException("dag id can't be empty");
            }
            if (tasks.isEmpty()) {
                throw new InvalidDagException("dag '" + id + "' has no tasks");
            }

            for (Task t : tasks.values()) {
                for (String dep : t.dependsOn()) {
                    if (!tasks.containsKey(dep)) {
                        throw new InvalidDagException(
                                "task '" + t.id() + "' depends on '" + dep + "' which doesn't exist");
                    }
                }
            }

            Dag dag = new Dag(id, new LinkedHashMap<>(tasks));
            dag.topologicalOrder(); // throws on cycle
            return dag;
        }
    }

    public static class InvalidDagException extends RuntimeException {
        public InvalidDagException(String message) {
            super(message);
        }
    }
}
