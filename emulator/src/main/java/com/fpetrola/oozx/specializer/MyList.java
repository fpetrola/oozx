package com.fpetrola.oozx.specializer;

import java.util.*;
import java.util.function.UnaryOperator;

public class MyList<S> implements List<S> {
  protected final List<S> internalList = new ArrayList<>();

  @Override
  public int size() {
    return internalList.size();
  }

  @Override
  public boolean isEmpty() {
    return internalList.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return internalList.contains(o);
  }

  @Override
  public Iterator<S> iterator() {
    return internalList.iterator();
  }

  @Override
  public Object[] toArray() {
    return internalList.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return internalList.toArray(a);
  }

  @Override
  public boolean add(S s) {
    return internalList.add(s);
  }

  @Override
  public boolean remove(Object o) {
    return internalList.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return internalList.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends S> c) {
    return internalList.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends S> c) {
    return internalList.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return internalList.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return internalList.retainAll(c);
  }

  @Override
  public void replaceAll(UnaryOperator<S> operator) {
    internalList.replaceAll(operator);
  }

  @Override
  public void sort(Comparator<? super S> c) {
    internalList.sort(c);
  }

  @Override
  public void clear() {
    internalList.clear();
  }

  @Override
  public boolean equals(Object o) {
    return internalList.equals(o);
  }

  @Override
  public int hashCode() {
    return internalList.hashCode();
  }

  @Override
  public S get(int index) {
    return internalList.get(index);
  }

  @Override
  public S set(int index, S element) {
    return internalList.set(index, element);
  }

  @Override
  public void add(int index, S element) {
    internalList.add(index, element);
  }

  @Override
  public S remove(int index) {
    return internalList.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return internalList.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return internalList.lastIndexOf(o);
  }

  @Override
  public ListIterator<S> listIterator() {
    return internalList.listIterator();
  }

  @Override
  public ListIterator<S> listIterator(int index) {
    return internalList.listIterator(index);
  }

  @Override
  public List<S> subList(int fromIndex, int toIndex) {
    return internalList.subList(fromIndex, toIndex);
  }

  @Override
  public Spliterator<S> spliterator() {
    return internalList.spliterator();
  }


}
