// Copyright (C) 2025, Imagic Bildverarbeitung AG, Sägereistrasse 29, CH-8152 Glattbrugg
//
// This file will be replaced as part of the open api generation process DO NOT EDIT
//
// This file is provided under the following conditions:
// THE SOFTWARE IS PROVIDED “AS IS” AND THE AUTHOR DISCLAIMS ALL
// WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES
// OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE
// FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY
// DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN
// AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
// OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
//
import java.util.Map;
import java.util.Objects;

public interface Visitable {

    /**
     * Returns a immutable iterable over all property keys of this Visitable.
     *
     * The property keys might not directy correspond to java field names if the property names
     * are not valid names for a java field.
     */
    Iterable<String> properties();

    /**
     * Returns the value in the object for the given property name.
     * Returns null if the property is null or does not exist.
     */
    Object get(String prop);

    /**
     * Returns the type of the property for the given property name.
     * The Type may be a simple Java Class or a ParameterizedType in case of generics such as List/Set.
     * Returns null if the property is null or does not exist.
     */
    java.lang.reflect.Type type(String prop);

    /**
     * Sets the property to the given value.
     * This method does nothing if the property is null or does not exist.
     * This method throws a ClassCastException if the value is not directly assignable (castable) to
     * the target type of the property.
     */
    void set(String prop, Object value);

    /**
     * Visit all properties of this element aswell as their child elements.
     */
    <E extends Throwable> void visit(PropertyVisitor<E> visitor) throws E;

    static <E extends Throwable> void visitObject(Visitable instance, PropertyVisitor<E> visitor, Object... properties) throws E {
        Objects.requireNonNull(instance, "instance must not be null");
        Objects.requireNonNull(visitor, "visitor must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        if ((properties.length & 1) != 0) {
            throw new IllegalArgumentException("properties.length must be even");
        }

        for (int i = 0; i < properties.length;) {
            String key = String.valueOf(properties[i++]);
            Object value = properties[i++];

            PropertyVisitor.PropertyVisitorAction act = visitor.visit(instance, key, value, value instanceof Visitable || value instanceof Map || value instanceof Iterable);

            switch (act) {
                case CONTINUE:
                    if (value instanceof Iterable) {
                        visitIterable(visitor, (Iterable<?>) value);
                        continue;
                    }
                    if (value instanceof Map) {
                        visitMap(visitor, (Map<?, ?>) value);
                        continue;
                    }
                    if (value instanceof Visitable) {
                        Visitable visitable = (Visitable) value;
                        visitable.visit(visitor);
                        continue;
                    }

                    continue;
                case SKIP_CHILDREN:
                    continue;
                case RETURN:
                    return;
            }
        }
    }

    static <E extends Throwable> void visitMap(PropertyVisitor<E> visitor, Map<?, ?> value) throws E {
        for (Map.Entry<?, ?> item : value.entrySet()) {
            if (!(item.getKey() instanceof String)) {
                //We don't visit Maps with non-string keys, because well, these can't exist in openapi.
                continue;
            }

            String key = item.getKey().toString();
            Object v = item.getValue();

            PropertyVisitor.PropertyVisitorAction act = visitor.visit(value, key, v, v instanceof Visitable || v instanceof Map || v instanceof Iterable);

            switch (act) {
                case CONTINUE:
                    if (v instanceof Visitable) {
                        Visitable visitable = (Visitable) v;
                        visitable.visit(visitor);
                        continue;
                    }

                    if (v instanceof Iterable) {
                        visitIterable(visitor, (Iterable<?>) v);
                        continue;
                    }

                    if (v instanceof Map) {
                        visitMap(visitor, (Map<?, ?>) v);
                        continue;
                    }

                    continue;
                case SKIP_CHILDREN:
                    continue;
                case RETURN:
                    return;
            }
        }
    }

    static <E extends Throwable> void visitIterable(PropertyVisitor<E> visitor, Iterable<?> value) throws E {

        int idx = -1;

        for (Object item : value) {
            idx++;
            PropertyVisitor.PropertyVisitorAction act = visitor.visit(value, String.valueOf(idx), item, item instanceof Visitable || item instanceof Map || item instanceof Iterable);

            switch (act) {
                case CONTINUE:
                    if (item instanceof Visitable) {
                        Visitable visitable = (Visitable) item;
                        visitable.visit(visitor);
                        continue;
                    }

                    if (item instanceof Iterable) {
                        visitIterable(visitor, (Iterable<?>) item);
                        continue;
                    }

                    if (item instanceof Map) {
                        visitMap(visitor, (Map<?, ?>) item);
                        continue;
                    }

                    continue;
                case SKIP_CHILDREN:
                    continue;
                case RETURN:
                    return;
            }
        }
    }
}
