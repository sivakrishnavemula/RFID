package org.apache.commons.collections4.list;

import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.apache.commons.collections4.OrderedIterator;

public class TreeList<E> extends AbstractList<E> {
   private TreeList.AVLNode<E> root;
   private int size;

   public TreeList() {
   }

   public TreeList(Collection<? extends E> coll) {
      if (!coll.isEmpty()) {
         this.root = new TreeList.AVLNode<>(coll);
         this.size = coll.size();
      }
   }

   public E get(int index) {
      this.checkInterval(index, 0, this.size() - 1);
      return this.root.get(index).getValue();
   }

   public int size() {
      return this.size;
   }

   public Iterator<E> iterator() {
      return this.listIterator(0);
   }

   public ListIterator<E> listIterator() {
      return this.listIterator(0);
   }

   public ListIterator<E> listIterator(int fromIndex) {
      this.checkInterval(fromIndex, 0, this.size());
      return new TreeList.TreeListIterator<>(this, fromIndex);
   }

   public int indexOf(Object object) {
      return this.root == null ? -1 : this.root.indexOf(object, this.root.relativePosition);
   }

   public boolean contains(Object object) {
      return this.indexOf(object) >= 0;
   }

   public Object[] toArray() {
      Object[] array = new Object[this.size()];
      if (this.root != null) {
         this.root.toArray(array, this.root.relativePosition);
      }

      return array;
   }

   public void add(int index, E obj) {
      this.modCount++;
      this.checkInterval(index, 0, this.size());
      if (this.root == null) {
         this.root = new TreeList.AVLNode<>(index, obj, null, null);
      } else {
         this.root = this.root.insert(index, obj);
      }

      this.size++;
   }

   public boolean addAll(Collection<? extends E> c) {
      if (c.isEmpty()) {
         return false;
      }

      this.modCount = this.modCount + c.size();
      TreeList.AVLNode<E> cTree = new TreeList.AVLNode<>(c);
      this.root = this.root == null ? cTree : this.root.addAll(cTree, this.size);
      this.size = this.size + c.size();
      return true;
   }

   public E set(int index, E obj) {
      this.checkInterval(index, 0, this.size() - 1);
      TreeList.AVLNode<E> node = this.root.get(index);
      E result = node.value;
      node.setValue(obj);
      return result;
   }

   public E remove(int index) {
      this.modCount++;
      this.checkInterval(index, 0, this.size() - 1);
      E result = this.get(index);
      this.root = this.root.remove(index);
      this.size--;
      return result;
   }

   public void clear() {
      this.modCount++;
      this.root = null;
      this.size = 0;
   }

   private void checkInterval(int index, int startIndex, int endIndex) {
      if (index < startIndex || index > endIndex) {
         throw new IndexOutOfBoundsException("Invalid index:" + index + ", size=" + this.size());
      }
   }

   static class AVLNode<E> {
      private TreeList.AVLNode<E> left;
      private boolean leftIsPrevious;
      private TreeList.AVLNode<E> right;
      private boolean rightIsNext;
      private int height;
      private int relativePosition;
      private E value;

      private AVLNode(int relativePosition, E obj, TreeList.AVLNode<E> rightFollower, TreeList.AVLNode<E> leftFollower) {
         this.relativePosition = relativePosition;
         this.value = obj;
         this.rightIsNext = true;
         this.leftIsPrevious = true;
         this.right = rightFollower;
         this.left = leftFollower;
      }

      private AVLNode(Collection<? extends E> coll) {
         this(coll.iterator(), 0, coll.size() - 1, 0, null, null);
      }

      private AVLNode(Iterator<? extends E> iterator, int start, int end, int absolutePositionOfParent, TreeList.AVLNode<E> prev, TreeList.AVLNode<E> next) {
         int mid = start + (end - start) / 2;
         if (start < mid) {
            this.left = new TreeList.AVLNode<>(iterator, start, mid - 1, mid, prev, this);
         } else {
            this.leftIsPrevious = true;
            this.left = prev;
         }

         this.value = (E)iterator.next();
         this.relativePosition = mid - absolutePositionOfParent;
         if (mid < end) {
            this.right = new TreeList.AVLNode<>(iterator, mid + 1, end, mid, this, next);
         } else {
            this.rightIsNext = true;
            this.right = next;
         }

         this.recalcHeight();
      }

      E getValue() {
         return this.value;
      }

      void setValue(E obj) {
         this.value = obj;
      }

      TreeList.AVLNode<E> get(int index) {
         int indexRelativeToMe = index - this.relativePosition;
         if (indexRelativeToMe == 0) {
            return this;
         }

         TreeList.AVLNode<E> nextNode = indexRelativeToMe < 0 ? this.getLeftSubTree() : this.getRightSubTree();
         return nextNode == null ? null : nextNode.get(indexRelativeToMe);
      }

      int indexOf(Object object, int index) {
         if (this.getLeftSubTree() != null) {
            int result = this.left.indexOf(object, index + this.left.relativePosition);
            if (result != -1) {
               return result;
            }
         }

         if (this.value == null ? this.value != object : !this.value.equals(object)) {
            return this.getRightSubTree() != null ? this.right.indexOf(object, index + this.right.relativePosition) : -1;
         } else {
            return index;
         }
      }

      void toArray(Object[] array, int index) {
         array[index] = this.value;
         if (this.getLeftSubTree() != null) {
            this.left.toArray(array, index + this.left.relativePosition);
         }

         if (this.getRightSubTree() != null) {
            this.right.toArray(array, index + this.right.relativePosition);
         }
      }

      TreeList.AVLNode<E> next() {
         return !this.rightIsNext && this.right != null ? this.right.min() : this.right;
      }

      TreeList.AVLNode<E> previous() {
         return !this.leftIsPrevious && this.left != null ? this.left.max() : this.left;
      }

      TreeList.AVLNode<E> insert(int index, E obj) {
         int indexRelativeToMe = index - this.relativePosition;
         return indexRelativeToMe <= 0 ? this.insertOnLeft(indexRelativeToMe, obj) : this.insertOnRight(indexRelativeToMe, obj);
      }

      private TreeList.AVLNode<E> insertOnLeft(int indexRelativeToMe, E obj) {
         if (this.getLeftSubTree() == null) {
            this.setLeft(new TreeList.AVLNode<>(-1, obj, this, this.left), null);
         } else {
            this.setLeft(this.left.insert(indexRelativeToMe, obj), null);
         }

         if (this.relativePosition >= 0) {
            this.relativePosition++;
         }

         TreeList.AVLNode<E> ret = this.balance();
         this.recalcHeight();
         return ret;
      }

      private TreeList.AVLNode<E> insertOnRight(int indexRelativeToMe, E obj) {
         if (this.getRightSubTree() == null) {
            this.setRight(new TreeList.AVLNode<>(1, obj, this.right, this), null);
         } else {
            this.setRight(this.right.insert(indexRelativeToMe, obj), null);
         }

         if (this.relativePosition < 0) {
            this.relativePosition--;
         }

         TreeList.AVLNode<E> ret = this.balance();
         this.recalcHeight();
         return ret;
      }

      private TreeList.AVLNode<E> getLeftSubTree() {
         return this.leftIsPrevious ? null : this.left;
      }

      private TreeList.AVLNode<E> getRightSubTree() {
         return this.rightIsNext ? null : this.right;
      }

      private TreeList.AVLNode<E> max() {
         return this.getRightSubTree() == null ? this : this.right.max();
      }

      private TreeList.AVLNode<E> min() {
         return this.getLeftSubTree() == null ? this : this.left.min();
      }

      TreeList.AVLNode<E> remove(int index) {
         int indexRelativeToMe = index - this.relativePosition;
         if (indexRelativeToMe == 0) {
            return this.removeSelf();
         }

         if (indexRelativeToMe > 0) {
            this.setRight(this.right.remove(indexRelativeToMe), this.right.right);
            if (this.relativePosition < 0) {
               this.relativePosition++;
            }
         } else {
            this.setLeft(this.left.remove(indexRelativeToMe), this.left.left);
            if (this.relativePosition > 0) {
               this.relativePosition--;
            }
         }

         this.recalcHeight();
         return this.balance();
      }

      private TreeList.AVLNode<E> removeMax() {
         if (this.getRightSubTree() == null) {
            return this.removeSelf();
         }

         this.setRight(this.right.removeMax(), this.right.right);
         if (this.relativePosition < 0) {
            this.relativePosition++;
         }

         this.recalcHeight();
         return this.balance();
      }

      private TreeList.AVLNode<E> removeMin() {
         if (this.getLeftSubTree() == null) {
            return this.removeSelf();
         }

         this.setLeft(this.left.removeMin(), this.left.left);
         if (this.relativePosition > 0) {
            this.relativePosition--;
         }

         this.recalcHeight();
         return this.balance();
      }

      private TreeList.AVLNode<E> removeSelf() {
         if (this.getRightSubTree() == null && this.getLeftSubTree() == null) {
            return null;
         }

         if (this.getRightSubTree() == null) {
            if (this.relativePosition > 0) {
               this.left.relativePosition = this.left.relativePosition + this.relativePosition;
            }

            this.left.max().setRight(null, this.right);
            return this.left;
         } else {
            if (this.getLeftSubTree() == null) {
               this.right.relativePosition = this.right.relativePosition + (this.relativePosition - (this.relativePosition < 0 ? 0 : 1));
               this.right.min().setLeft(null, this.left);
               return this.right;
            }

            if (this.heightRightMinusLeft() > 0) {
               TreeList.AVLNode<E> rightMin = this.right.min();
               this.value = rightMin.value;
               if (this.leftIsPrevious) {
                  this.left = rightMin.left;
               }

               this.right = this.right.removeMin();
               if (this.relativePosition < 0) {
                  this.relativePosition++;
               }
            } else {
               TreeList.AVLNode<E> leftMax = this.left.max();
               this.value = leftMax.value;
               if (this.rightIsNext) {
                  this.right = leftMax.right;
               }

               TreeList.AVLNode<E> leftPrevious = this.left.left;
               this.left = this.left.removeMax();
               if (this.left == null) {
                  this.left = leftPrevious;
                  this.leftIsPrevious = true;
               }

               if (this.relativePosition > 0) {
                  this.relativePosition--;
               }
            }

            this.recalcHeight();
            return this;
         }
      }

      private TreeList.AVLNode<E> balance() {
         switch (this.heightRightMinusLeft()) {
            case -2:
               if (this.left.heightRightMinusLeft() > 0) {
                  this.setLeft(this.left.rotateLeft(), null);
               }

               return this.rotateRight();
            case -1:
            case 0:
            case 1:
               return this;
            case 2:
               if (this.right.heightRightMinusLeft() < 0) {
                  this.setRight(this.right.rotateRight(), null);
               }

               return this.rotateLeft();
            default:
               throw new RuntimeException("tree inconsistent!");
         }
      }

      private int getOffset(TreeList.AVLNode<E> node) {
         return node == null ? 0 : node.relativePosition;
      }

      private int setOffset(TreeList.AVLNode<E> node, int newOffest) {
         if (node == null) {
            return 0;
         }

         int oldOffset = this.getOffset(node);
         node.relativePosition = newOffest;
         return oldOffset;
      }

      private void recalcHeight() {
         this.height = Math.max(
               this.getLeftSubTree() == null ? -1 : this.getLeftSubTree().height, this.getRightSubTree() == null ? -1 : this.getRightSubTree().height
            )
            + 1;
      }

      private int getHeight(TreeList.AVLNode<E> node) {
         return node == null ? -1 : node.height;
      }

      private int heightRightMinusLeft() {
         return this.getHeight(this.getRightSubTree()) - this.getHeight(this.getLeftSubTree());
      }

      private TreeList.AVLNode<E> rotateLeft() {
         TreeList.AVLNode<E> newTop = this.right;
         TreeList.AVLNode<E> movedNode = this.getRightSubTree().getLeftSubTree();
         int newTopPosition = this.relativePosition + this.getOffset(newTop);
         int myNewPosition = -newTop.relativePosition;
         int movedPosition = this.getOffset(newTop) + this.getOffset(movedNode);
         this.setRight(movedNode, newTop);
         newTop.setLeft(this, null);
         this.setOffset(newTop, newTopPosition);
         this.setOffset(this, myNewPosition);
         this.setOffset(movedNode, movedPosition);
         return newTop;
      }

      private TreeList.AVLNode<E> rotateRight() {
         TreeList.AVLNode<E> newTop = this.left;
         TreeList.AVLNode<E> movedNode = this.getLeftSubTree().getRightSubTree();
         int newTopPosition = this.relativePosition + this.getOffset(newTop);
         int myNewPosition = -newTop.relativePosition;
         int movedPosition = this.getOffset(newTop) + this.getOffset(movedNode);
         this.setLeft(movedNode, newTop);
         newTop.setRight(this, null);
         this.setOffset(newTop, newTopPosition);
         this.setOffset(this, myNewPosition);
         this.setOffset(movedNode, movedPosition);
         return newTop;
      }

      private void setLeft(TreeList.AVLNode<E> node, TreeList.AVLNode<E> previous) {
         this.leftIsPrevious = node == null;
         this.left = this.leftIsPrevious ? previous : node;
         this.recalcHeight();
      }

      private void setRight(TreeList.AVLNode<E> node, TreeList.AVLNode<E> next) {
         this.rightIsNext = node == null;
         this.right = this.rightIsNext ? next : node;
         this.recalcHeight();
      }

      private TreeList.AVLNode<E> addAll(TreeList.AVLNode<E> otherTree, int currentSize) {
         TreeList.AVLNode<E> maxNode = this.max();
         TreeList.AVLNode<E> otherTreeMin = otherTree.min();
         if (otherTree.height > this.height) {
            TreeList.AVLNode<E> leftSubTree = this.removeMax();
            Deque<TreeList.AVLNode<E>> sAncestors = new ArrayDeque();
            TreeList.AVLNode<E> s = otherTree;
            int sAbsolutePosition = s.relativePosition + currentSize;
            int sParentAbsolutePosition = 0;

            while (s != null && s.height > this.getHeight(leftSubTree)) {
               sParentAbsolutePosition = sAbsolutePosition;
               sAncestors.push(s);
               s = s.left;
               if (s != null) {
                  sAbsolutePosition += s.relativePosition;
               }
            }

            maxNode.setLeft(leftSubTree, null);
            maxNode.setRight(s, otherTreeMin);
            if (leftSubTree != null) {
               leftSubTree.max().setRight(null, maxNode);
               leftSubTree.relativePosition -= currentSize - 1;
            }

            if (s != null) {
               s.min().setLeft(null, maxNode);
               s.relativePosition = sAbsolutePosition - currentSize + 1;
            }

            maxNode.relativePosition = currentSize - 1 - sParentAbsolutePosition;
            otherTree.relativePosition += currentSize;
            s = maxNode;

            while (!sAncestors.isEmpty()) {
               TreeList.AVLNode<E> sAncestor = (TreeList.AVLNode<E>)sAncestors.pop();
               sAncestor.setLeft(s, null);
               s = sAncestor.balance();
            }

            return s;
         } else {
            otherTree = otherTree.removeMin();
            Deque<TreeList.AVLNode<E>> sAncestors = new ArrayDeque();
            TreeList.AVLNode<E> s = this;
            int sAbsolutePosition = s.relativePosition;
            int sParentAbsolutePosition = 0;

            while (s != null && s.height > this.getHeight(otherTree)) {
               sParentAbsolutePosition = sAbsolutePosition;
               sAncestors.push(s);
               s = s.right;
               if (s != null) {
                  sAbsolutePosition += s.relativePosition;
               }
            }

            otherTreeMin.setRight(otherTree, null);
            otherTreeMin.setLeft(s, maxNode);
            if (otherTree != null) {
               otherTree.min().setLeft(null, otherTreeMin);
               otherTree.relativePosition++;
            }

            if (s != null) {
               s.max().setRight(null, otherTreeMin);
               s.relativePosition = sAbsolutePosition - currentSize;
            }

            otherTreeMin.relativePosition = currentSize - sParentAbsolutePosition;
            s = otherTreeMin;

            while (!sAncestors.isEmpty()) {
               TreeList.AVLNode<E> sAncestor = (TreeList.AVLNode<E>)sAncestors.pop();
               sAncestor.setRight(s, null);
               s = sAncestor.balance();
            }

            return s;
         }
      }

      public String toString() {
         return "AVLNode("
            + this.relativePosition
            + ','
            + (this.left != null)
            + ','
            + this.value
            + ','
            + (this.getRightSubTree() != null)
            + ", faedelung "
            + this.rightIsNext
            + " )";
      }
   }

   static class TreeListIterator<E> implements ListIterator<E>, OrderedIterator<E> {
      private final TreeList<E> parent;
      private TreeList.AVLNode<E> next;
      private int nextIndex;
      private TreeList.AVLNode<E> current;
      private int currentIndex;
      private int expectedModCount;

      protected TreeListIterator(TreeList<E> parent, int fromIndex) throws IndexOutOfBoundsException {
         this.parent = parent;
         this.expectedModCount = parent.modCount;
         this.next = parent.root == null ? null : parent.root.get(fromIndex);
         this.nextIndex = fromIndex;
         this.currentIndex = -1;
      }

      protected void checkModCount() {
         if (this.parent.modCount != this.expectedModCount) {
            throw new ConcurrentModificationException();
         }
      }

      public boolean hasNext() {
         return this.nextIndex < this.parent.size();
      }

      public E next() {
         this.checkModCount();
         if (!this.hasNext()) {
            throw new NoSuchElementException("No element at index " + this.nextIndex + ".");
         }

         if (this.next == null) {
            this.next = this.parent.root.get(this.nextIndex);
         }

         E value = this.next.getValue();
         this.current = this.next;
         this.currentIndex = this.nextIndex++;
         this.next = this.next.next();
         return value;
      }

      public boolean hasPrevious() {
         return this.nextIndex > 0;
      }

      public E previous() {
         this.checkModCount();
         if (!this.hasPrevious()) {
            throw new NoSuchElementException("Already at start of list.");
         }

         if (this.next == null) {
            this.next = this.parent.root.get(this.nextIndex - 1);
         } else {
            this.next = this.next.previous();
         }

         E value = this.next.getValue();
         this.current = this.next;
         this.currentIndex = --this.nextIndex;
         return value;
      }

      public int nextIndex() {
         return this.nextIndex;
      }

      public int previousIndex() {
         return this.nextIndex() - 1;
      }

      public void remove() {
         this.checkModCount();
         if (this.currentIndex == -1) {
            throw new IllegalStateException();
         }

         this.parent.remove(this.currentIndex);
         if (this.nextIndex != this.currentIndex) {
            this.nextIndex--;
         }

         this.next = null;
         this.current = null;
         this.currentIndex = -1;
         this.expectedModCount++;
      }

      public void set(E obj) {
         this.checkModCount();
         if (this.current == null) {
            throw new IllegalStateException();
         }

         this.current.setValue(obj);
      }

      public void add(E obj) {
         this.checkModCount();
         this.parent.add(this.nextIndex, obj);
         this.current = null;
         this.currentIndex = -1;
         this.nextIndex++;
         this.expectedModCount++;
      }
   }
}
