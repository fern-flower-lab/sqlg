package org.umlg.sqlg.test.ltree;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.umlg.sqlg.predicate.Lquery;
import org.umlg.sqlg.predicate.LqueryArray;
import org.umlg.sqlg.structure.PropertyDefinition;
import org.umlg.sqlg.structure.PropertyType;
import org.umlg.sqlg.test.BaseTest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestPostgresLtree extends BaseTest {

    @Test
    public void testLTreeLqueryArray() {
        Assume.assumeTrue(isPostgres());
        this.sqlgGraph.getTopology().getPublicSchema()
                .ensureVertexLabelExist("Tree", new HashMap<>() {{
                    put("path", PropertyDefinition.of(PropertyType.LTREE));
                }});
        this.sqlgGraph.tx().commit();
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one.two");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one.two.one");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one.two.two");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one.three");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one.three.one");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one.three.two");
        this.sqlgGraph.tx().commit();

        List<Vertex> tree = this.sqlgGraph.traversal().V().hasLabel("Tree")
                .has("path", Lquery.descendantOfRightOrEquals("one.two"))
                .toList();
        Assert.assertEquals(3, tree.size());

        tree = this.sqlgGraph.traversal().V().hasLabel("Tree")
                .has("path", Lquery.descendantOfRightOrEquals("one.three"))
                .toList();
        Assert.assertEquals(3, tree.size());

        tree = this.sqlgGraph.traversal().V().hasLabel("Tree")
                .has("path", LqueryArray.descendantOfRightOrEquals(new String[]{"one.two", "one.three"}))
                .toList();
        Assert.assertEquals(6, tree.size());

        tree = this.sqlgGraph.traversal().V().hasLabel("Tree")
                .has("path", LqueryArray.ancestorOfRightOrEquals(new String[]{"one.two", "one.three"}))
                .toList();
        Assert.assertEquals(3, tree.size());

    }

    @Test
    public void testLTreeLquery() {
        Assume.assumeTrue(isPostgres());
        this.sqlgGraph.getTopology().getPublicSchema()
                .ensureVertexLabelExist("Tree", new HashMap<>() {{
                    put("path", PropertyDefinition.of(PropertyType.LTREE));
                }});
        this.sqlgGraph.tx().commit();
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one.two");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one.two.three");
        this.sqlgGraph.tx().commit();
        List<Vertex> tree = this.sqlgGraph.traversal().V().hasLabel("Tree")
                .has("path", Lquery.descendantOfRightOrEquals("one.two"))
                .toList();
        Assert.assertEquals(2, tree.size());

        tree = this.sqlgGraph.traversal().V().hasLabel("Tree")
                .has("path", Lquery.ancestorOfRightOrEquals("one.two"))
                .toList();
        Assert.assertEquals(2, tree.size());

        tree = this.sqlgGraph.traversal().V().hasLabel("Tree")
                .has("path", Lquery.lquery("one.two.*"))
                .toList();
        Assert.assertEquals(2, tree.size());

        tree = this.sqlgGraph.traversal().V().hasLabel("Tree")
                .has("path", Lquery.lquery("one.*{1}"))
                .toList();
        Assert.assertEquals(1, tree.size());

//        Connection connection = sqlgGraph.tx().getConnection();
//        try (PreparedStatement preparedStatement = connection.prepareStatement("select path from \"V_Tree\" where path ~ ?")) {
//            sqlgGraph.getSqlDialect().setLquery(preparedStatement, 1, "one.*");
//            ResultSet rs = preparedStatement.executeQuery();
//            while (rs.next()) {
//                System.out.println(rs.getString(1));
//            }
//        }
    }

    @Test
    public void testLTreeLqueryUpToLevel() {
        Assume.assumeTrue(isPostgres());
        this.sqlgGraph.getTopology().getPublicSchema()
                .ensureVertexLabelExist("Tree", new HashMap<>() {{
                    put("path", PropertyDefinition.of(PropertyType.LTREE));
                }});
        this.sqlgGraph.tx().commit();
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one1");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one1.two11");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one1.two12");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one1.two13");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one1.two11.three111");

        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one2");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one2.two12");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one2.two22");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one2.two23");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one2.two12.three121");
        this.sqlgGraph.tx().commit();

        List<Vertex> tree = this.sqlgGraph.traversal().V().hasLabel("Tree")
                .has("path", Lquery.lquery("one1.*{1}"))
                .toList();
        Assert.assertEquals(3, tree.size());

        tree = this.sqlgGraph.traversal().V().hasLabel("Tree")
                .has("path", Lquery.lquery("*{0,2}"))
                .toList();
        Assert.assertEquals(8, tree.size());

        List<Vertex> theRestTreeItems = sqlgGraph.traversal().V().hasLabel("Tree")
                .and(
                        __.has("path", Lquery.lquery("*{0,2}")),
                        __.has("path", P.neq("one2"))
                )
                .toList();
        Assert.assertEquals(7, theRestTreeItems.size());
    }

    @Test
    public void testLTreeSetNull() {
        Assume.assumeTrue(isPostgres());
        this.sqlgGraph.getTopology().getPublicSchema()
                .ensureVertexLabelExist("Tree", new HashMap<>() {{
                    put("path", PropertyDefinition.of(PropertyType.LTREE));
                }});
        this.sqlgGraph.tx().commit();
        this.sqlgGraph.addVertex(T.label, "Tree", "path", null);
        this.sqlgGraph.tx().commit();
        List<Vertex> tree = this.sqlgGraph.traversal().V().hasLabel("Tree").toList();
        Assert.assertEquals(1, tree.size());
    }

    @Test
    public void testLTree() {
        Assume.assumeTrue(isPostgres());
        this.sqlgGraph.getTopology().getPublicSchema()
                .ensureVertexLabelExist("Tree", new HashMap<>() {{
                    put("path", PropertyDefinition.of(PropertyType.LTREE));
                }});
        this.sqlgGraph.tx().commit();
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one.two");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one.two.three");
        this.sqlgGraph.tx().commit();
        List<Vertex> tree = this.sqlgGraph.traversal().V().hasLabel("Tree").toList();
        Assert.assertEquals(3, tree.size());
        Set<String> paths = new HashSet<>(Set.of("one", "one.two", "one.two.three"));
        for (Vertex t : tree) {
            String path = t.value("path");
            paths.remove(path);
        }
        Assert.assertTrue(paths.isEmpty());
    }

    @Test
    public void testLTreeWithin() {
        Assume.assumeTrue(isPostgres());
        this.sqlgGraph.getTopology().getPublicSchema()
                .ensureVertexLabelExist("Tree", new HashMap<>() {{
                    put("path", PropertyDefinition.of(PropertyType.LTREE));
                }});
        this.sqlgGraph.tx().commit();
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one.two");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one.two.three");
        this.sqlgGraph.addVertex(T.label, "Tree", "path", "one.two.four");
        this.sqlgGraph.tx().commit();
        List<Vertex> tree = this.sqlgGraph.traversal().V()
                .hasLabel("Tree")
                .has("path", P.within("one.two", "one.two.three", "one.two.four"))
                .toList();
        Assert.assertEquals(3, tree.size());
    }
}
