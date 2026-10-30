package io.warden.dag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DagTest {

    @Test
    void ordersTasksByDependencies() {
        Dag dag = Dag.builder("daily_sales")
                .task("extract")
                .task("clean", "extract")
                .task("enrich", "extract")
                .task("load", "clean", "enrich")
                .build();

        List<String> order = dag.topologicalOrder().stream().map(Task::id).toList();

        assertEquals("extract", order.get(0));
        assertEquals("load", order.get(3));
        assertTrue(order.indexOf("clean") < order.indexOf("load"));
        assertTrue(order.indexOf("enrich") < order.indexOf("load"));
    }

    @Test
    void findsRootsAndDownstream() {
        Dag dag = Dag.builder("x")
                .task("a")
                .task("b", "a")
                .task("c", "a")
                .build();

        assertEquals(List.of("a"), dag.roots().stream().map(Task::id).toList());
        assertEquals(2, dag.downstreamOf("a").size());
        assertTrue(dag.downstreamOf("c").isEmpty());
    }

    @Test
    void failsOnCycle() {
        var builder = Dag.builder("loop")
                .task("a", "c")
                .task("b", "a")
                .task("c", "b");

        var ex = assertThrows(Dag.InvalidDagException.class, builder::build);
        assertTrue(ex.getMessage().contains("cycle"));
    }

    @Test
    void failsOnMissingDependency() {
        var builder = Dag.builder("broken")
                .task("load", "transform");

        assertThrows(Dag.InvalidDagException.class, builder::build);
    }

    @Test
    void failsOnDuplicateTask() {
        assertThrows(Dag.InvalidDagException.class, () ->
                Dag.builder("dup").task("a").task("a"));
    }
}
