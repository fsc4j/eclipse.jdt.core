package fsc4j;

import java.util.function.Consumer;

public interface RelatedObjectIterator<T> {
	void forEachRelatedObject(T subject, Consumer<Object> consumer);
}