package me.toomuchzelda.teamarenapaper.utils;

import java.util.*;

public class ShufflingQueue<T>
{
	ArrayList<T> queue;

	//remove from one, insert in random order to other.
	ArrayList<T> queueOne;
	ArrayList<T> queueTwo;

	public ShufflingQueue(Collection<T> elems) {
		this.queueOne = new ArrayList<>(elems);
		this.queueTwo = new ArrayList<>(elems.size());
		this.queue = queueOne;

		Collections.shuffle(queue, MathUtils.random);
	}

	public T poll() {
		T chosen = queue.remove(queue.size() - 1);
		ArrayList<T> otherQueue = queue == queueOne ? queueTwo : queueOne;
		//put the chosen map into a random place in the other queue (queue to be used after current one
		// is depleted)
		otherQueue.add(MathUtils.randomMax(otherQueue.size()), chosen);
		//played the whole queue, restart
		if (queue.isEmpty()) {
			queue = otherQueue;
		}

		return chosen;
	}

	// Get next few maps to vote on.
	public T peek(int index) {
		if (index >= (this.size())) {
			throw new IllegalArgumentException("amount may not be greater than the number of maps");
		}

		ArrayList<T> otherQueue = queue == queueOne ? queueTwo : queueOne;
		if (index < queue.size()) {
			return queue.get(index);
		}
		else {
			return otherQueue.get(index - queue.size());
		}
	}

	public void shuffleSingleElem(final T elem) {
		if (queueOne.remove(elem)) {
			queueTwo.add(MathUtils.randomMax(queueTwo.size()), elem);
		}
		else if (queueTwo.remove(elem)) {
			queueOne.add(MathUtils.randomMax(queueOne.size()), elem);
		}
		else {
			throw new NoSuchElementException("ShufflingQueue");
		}
	}

	public int size() {
		return this.queueOne.size() + this.queueTwo.size();
	}

	/** Re-shuffle */
	public void shuffle() {
		while (!queueTwo.isEmpty()) {
			queueOne.add(queueTwo.remove(0));
		}
		Collections.shuffle(queueOne);

		queue = queueOne;
	}

	/** Returns a copy */
	public List<T> getElements() {
		ArrayList<T> all = new ArrayList<>(queueOne.size() + queueTwo.size());
		all.addAll(queueOne);
		all.addAll(queueTwo);
		return all;
	}
}
