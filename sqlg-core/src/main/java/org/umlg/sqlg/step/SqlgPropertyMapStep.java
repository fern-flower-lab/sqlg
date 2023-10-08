package org.umlg.sqlg.step;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.step.TraversalParent;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.WithOptions;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.TraverserRequirement;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalProduct;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalRing;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalUtil;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.*;

/**
 * @author <a href="https://github.com/pietermartin">Pieter Martin</a>
 * Date: 2018/08/25
 */
public class SqlgPropertyMapStep<K, E> extends SqlgMapStep<Element, Map<K, E>> implements TraversalParent {

    protected final String[] propertyKeys;
    protected final PropertyType returnType;
    protected final int tokens;
    protected Traversal.Admin<Element, ? extends Property> propertyTraversal;
    private final TraversalRing<K, E> traversalRing;

    public SqlgPropertyMapStep(final Traversal.Admin traversal, final int tokens, final PropertyType propertyType,final TraversalRing<K, E> traversalRing,  final String... propertyKeys) {
        super(traversal);
        this.tokens = tokens;
        this.propertyKeys = propertyKeys;
        this.returnType = propertyType;
        this.propertyTraversal = null;
        this.traversalRing = traversalRing;
    }

    @Override
    protected Map<K, E> map(final Traverser.Admin<Element> traverser) {
        final Map<Object, Object> map = new HashMap<>();
        final Element element = traverser.get();
        final boolean isVertex = traverser.get() instanceof Vertex;
        final Iterator<? extends Property> properties = null == this.propertyTraversal ?
                element.properties(this.propertyKeys) :
                TraversalUtil.applyAll(traverser, this.propertyTraversal);
        while (properties.hasNext()) {
            final Property<?> property = properties.next();
            if (isVertex) {
                List<Object> values = (List<Object>) map.get(property.key());
                if (null == values) {
                    values = new ArrayList<>();
                    map.put(property.key(), values);
                }
                values.add(this.returnType == PropertyType.VALUE ? property.value() : property);
            } else
                map.put(property.key(), this.returnType == PropertyType.VALUE ? property.value() : property);
        }
        if (this.returnType == PropertyType.VALUE) {
            if (includeToken(WithOptions.ids)) map.put(T.id, element.id());
            if (element instanceof VertexProperty) {
                if (includeToken(WithOptions.keys)) {
                    map.put(T.key, ((VertexProperty<?>) element).key());
                }
                if (includeToken(WithOptions.values)) {
                    map.put(T.value, ((VertexProperty<?>) element).value());
                }
            } else {
                if (includeToken(WithOptions.labels)) {
                    map.put(T.label, element.label());
                }
            }
        }
        if (!traversalRing.isEmpty()) {
            final Set<Object> keys = new HashSet<>(map.keySet());
            for (final Object key : keys) {
                map.compute(key, (k, v) -> {
                    TraversalProduct traversalProduct = TraversalUtil.produce(v, (Traversal.Admin) this.traversalRing.next());
                    if (traversalProduct.isProductive()) {
                        return traversalProduct.get();
                    } else {
                        return null;
                    }
                });
            }
            this.traversalRing.reset();
        }
        return (Map) map;
    }

    @Override
    public List<Traversal.Admin<Element, ? extends Property>> getLocalChildren() {
        return null == this.propertyTraversal ? Collections.emptyList() : Collections.singletonList(this.propertyTraversal);
    }

    public String toString() {
        return null != this.propertyTraversal ?
                StringFactory.stepString(this, this.propertyTraversal, this.returnType.name().toLowerCase()) :
                StringFactory.stepString(this, Arrays.asList(this.propertyKeys), this.returnType.name().toLowerCase());
    }

    @Override
    public SqlgPropertyMapStep<K, E> clone() {
        final SqlgPropertyMapStep<K, E> clone = (SqlgPropertyMapStep<K, E>) super.clone();
        if (null != this.propertyTraversal)
            clone.propertyTraversal = this.propertyTraversal.clone();
        return clone;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode() ^ this.returnType.hashCode() ^ Integer.hashCode(this.tokens);
        if (null == this.propertyTraversal) {
            //noinspection ConfusingArgumentToVarargsMethod
            return result ^ Objects.hash(this.propertyKeys);
        } else {
            return result ^ this.propertyTraversal.hashCode();
        }
    }

    @Override
    public void setTraversal(final Traversal.Admin<?, ?> parentTraversal) {
        super.setTraversal(parentTraversal);
        if (null != this.propertyTraversal)
            this.integrateChild(this.propertyTraversal);
    }

    @Override
    public Set<TraverserRequirement> getRequirements() {
        return this.getSelfAndChildRequirements(TraverserRequirement.OBJECT);
    }

    private boolean includeToken(final int token) {
        return 0 != (this.tokens & token);
    }
}
