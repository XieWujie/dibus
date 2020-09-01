package com.dibus;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;

 class WeakEventQueue<E> {
    /**
     * Reference queue for cleared weak references
     */
    private final ReferenceQueue garbage = new ReferenceQueue<>();

    /**
     * Strongly referenced list head
     */
    private Object strongRef = new Object();
    private ListEntry head = new ListEntry(strongRef, garbage,null);

    /**
     * Size of the queue
     */
    int size = 0;

    @SuppressWarnings("unchecked")
    public void add(E obj,EventExecutor<E>eventExecutor) {
        cleanup();
        size++;
        new ListEntry(obj, garbage,eventExecutor).insert(head.prev);
    }

    public void remove(E obj) {
        cleanup();

        ListEntry entry = head.next;
        while (entry != head) {
            Object other = entry.get();
            if (other == obj) {
                size--;
                entry.remove();
                return;
            }
            entry = entry.next;
        }
    }

    public void cleanup() {
        ListEntry entry;
        while ((entry = (ListEntry) garbage.poll()) != null) {
            size--;
            entry.remove();
        }
    }

    public Iterator<ListEntry> iterator() {
        return new Iterator() {
            private ListEntry index = head;
            private ListEntry next = null;

            public boolean hasNext() {
                next = null;
                while (next == null) {
                    ListEntry nextIndex = index.prev;
                    if (nextIndex == head) {
                        break;
                    }
                    next = nextIndex;
                    if (next == null) {
                        size--;
                        nextIndex.remove();
                    }
                }

                return next != null;
            }

            public ListEntry next() {
                hasNext(); // forces us to clear out crap up to the next
                           // valid spot
                index = index.prev;
                return next;
            }

            public void remove() {
                if (index != head) {
                    ListEntry nextIndex = index.next;
                    size--;
                    index.remove();
                    index = nextIndex;
                }
            }
        };
    }

    static class ListEntry extends WeakReference {
        private ListEntry prev, next;
        private EventExecutor eventExecutor;

        public ListEntry(Object o, ReferenceQueue queue,EventExecutor eventExecutor) {
            super(o, queue);
            prev = this;
            next = this;
            this.eventExecutor = eventExecutor;
        }

        public void execute(Object ...args){
            eventExecutor.execute(get(),args);
        }

        public void insert(ListEntry where) {
            prev = where;
            next = where.next;
            where.next = this;
            next.prev = this;
        }

        public void remove() {
            prev.next = next;
            next.prev = prev;
            next = this;
            prev = this;
        }
    }

}