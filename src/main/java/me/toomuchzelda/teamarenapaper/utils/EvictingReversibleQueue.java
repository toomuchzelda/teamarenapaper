package me.toomuchzelda.teamarenapaper.utils;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ForwardingQueue;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link EvictingQueue EvictingQueue} that also exposes the descendingIterator
 *
 * @param <E>
 */
public final class EvictingReversibleQueue<E> extends ForwardingQueue<E> {

	private final ArrayDeque<E> delegate;
	final int maxSize;

	private EvictingReversibleQueue(int maxSize) {
		checkArgument(maxSize >= 0, "maxSize (%s) must >= 0", maxSize);
		this.delegate = new ArrayDeque<>(maxSize);
		this.maxSize = maxSize;
	}

	/**
	 * Creates and returns a new evicting queue that will hold up to {@code maxSize} elements.
	 *
	 * <p>When {@code maxSize} is zero, elements will be evicted immediately after being added to the
	 * queue.
	 */
	public static <E> EvictingReversibleQueue<E> create(int maxSize) {
		return new EvictingReversibleQueue<>(maxSize);
	}

	/**
	 * Returns the number of additional elements that this queue can accept without evicting; zero if
	 * the queue is currently full.
	 *
	 * @since 16.0
	 */
	public int remainingCapacity() {
		return maxSize - size();
	}

	@Override
	protected @NotNull Queue<E> delegate() {
		return delegate;
	}

	/**
	 * Adds the given element to this queue. If the queue is currently full, the element at the head
	 * of the queue is evicted to make room.
	 *
	 * @return {@code true} always
	 */
	@Override
	@CanIgnoreReturnValue
	public boolean offer(E e) {
		return add(e);
	}

	/**
	 * Adds the given element to this queue. If the queue is currently full, the element at the head
	 * of the queue is evicted to make room.
	 *
	 * @return {@code true} always
	 */
	@Override
	@CanIgnoreReturnValue
	public boolean add(E e) {
		checkNotNull(e); // check before removing
		if (maxSize == 0) {
			return true;
		}
		if (size() == maxSize) {
			delegate.remove();
		}
		delegate.add(e);
		return true;
	}

	@Override
	@CanIgnoreReturnValue
	public boolean addAll(Collection<? extends E> collection) {
		int size = collection.size();
		if (size >= maxSize) {
			clear();
			return Iterables.addAll(this, Iterables.skip(collection, size - maxSize));
		}
		return standardAddAll(collection);
	}

	public Iterator<E> descendingIterator() {
		return delegate.descendingIterator();
	}
}
