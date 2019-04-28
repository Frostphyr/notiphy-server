package com.frostphyr.notiphy.util;

import java.util.Deque;
import java.util.LinkedList;

public class FixedLinkedStack<T> {
	
	private Deque<T> list = new LinkedList<>();
	private int capacity;
	
	public FixedLinkedStack(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("capacity must be >= 0");
		}
		this.capacity = capacity;
	}
	
	public void push(T value) {
		if (list.offerFirst(value) && list.size() > capacity) {
			list.removeLast();
		}
	}
	
	public T pop() {
		return list.pollFirst();
	}
	
	public T peek() {
		return list.peekFirst();
	}
	
	public int getSize() {
		return list.size();
	}
	
	public int getCapacity() {
		return capacity;
	}

}
