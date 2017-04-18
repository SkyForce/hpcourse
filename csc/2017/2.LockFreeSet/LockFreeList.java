import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * Created by dvolkow on 17.04.17.
 */
public class LockFreeList<Type extends Comparable<Type>> implements LockFreeSet<Type> {

    private Node head_;

    public LockFreeList() {
        head_ = new Node(new AtomicMarkableReference<Node>(null, false), null);
    }

    private NodePair find(Type key) {
        Node prev = head_;
        Node current = head_.next().getReference();

        while (current != null) {
            Node succ = current.next().getReference();

            if (current.next().isMarked()) {
                if (!prev.next().compareAndSet(current, succ, false, false))
                    return find(key);

            } else {
                if (current.getKey().compareTo(key) >= 0)
                    return new NodePair(prev, current);

                prev = current;
            }

            current = succ;
        }

        return new NodePair(prev, null);
    }

    @Override
    public boolean add(Type key) {
        while (true) {
            NodePair nodePair = find(key);
            Node prev = nodePair.prev();
            Node current = nodePair.current();

            if (current != null && current.getKey().compareTo(key) == 0)
                return false;

            Node node = new Node(new AtomicMarkableReference<Node>(current, false), key);
            if (prev.next().compareAndSet(current, node, false, false))
                return true;
        }
    }

    @Override
    public boolean remove(Type key) {
        while (true) {
            NodePair nodePair = find(key);
            Node prev = nodePair.prev();
            Node current = nodePair.current();

            if (current == null || current.getKey().compareTo(key) != 0)
                return false;

            Node succ = current.next().getReference();
            if (current.next().attemptMark(succ, true)) {
                prev.next().compareAndSet(current, succ, false, false);
                return true;
            }
        }
    }

    @Override
    public boolean contains(Type key) {
        Node current = head_.next().getReference();

        while (current != null && current.getKey().compareTo(key) < 0)
            current = current.next().getReference();

        return current != null && !current.next().isMarked() && current.getKey().compareTo(key) == 0;
    }

    @Override
    public boolean isEmpty() {
        //--unfair
        return head_.next().getReference() == null;
    }

    private class Node {
        private AtomicMarkableReference<Node> next_;
        private Type key_;

        Node(AtomicMarkableReference<Node> next, Type key) {
            next_ = next;
            key_ = key;
        }

        public Node getNext() {
            return next_.getReference();
        }

        public AtomicMarkableReference<Node> next() {
            return next_;
        }

        public Type getKey() {
            return key_;
        }
    }

    class NodePair {
        private Node prev_;
        private Node current_;

        NodePair(Node prev, Node current) {
            prev_ = prev;
            current_ = current;
        }

        public Node prev() {
            return prev_;
        }

        public Node current() {
            return current_;
        }
    }
}
