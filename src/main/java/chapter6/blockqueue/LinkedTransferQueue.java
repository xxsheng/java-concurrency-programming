/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

package chapter6.blockqueue;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * An unbounded {@link TransferQueue} based on linked nodes.
 * This queue orders elements FIFO (first-in-first-out) with respect
 * to any given producer.  The <em>head</em> of the queue is that
 * element that has been on the queue the longest time for some
 * producer.  The <em>tail</em> of the queue is that element that has
 * been on the queue the shortest time for some producer.
 *
 * <p>Beware that, unlike in most collections, the {@code size}
 * method is <em>NOT</em> a constant-time operation. Because of the
 * asynchronous nature of these queues, determining the current number
 * of elements requires a traversal of the elements.
 *
 * <p>This class and its iterator implement all of the
 * <em>optional</em> methods of the {@link Collection} and {@link
 * Iterator} interfaces.
 *
 * <p>Memory consistency effects: As with other concurrent
 * collections, actions in a thread prior to placing an object into a
 * {@code LinkedTransferQueue}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that element from
 * the {@code LinkedTransferQueue} in another thread.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.7
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public class LinkedTransferQueue<E> extends AbstractQueue<E>
    implements TransferQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -3223113410248163686L;

    /*
     * *** Overview of Dual Queues with Slack ***
     *
     * Dual Queues, introduced by Scherer and Scott
     * (http://www.cs.rice.edu/~wns1/papers/2004-DISC-DDS.pdf) are
     * (linked) queues in which nodes may represent either data or
     * requests.  When a thread tries to enqueue a data node, but
     * encounters a request node, it instead "matches" and removes it;
     * and vice versa for enqueuing requests. Blocking Dual Queues
     * arrange that threads enqueuing unmatched requests block until
     * other threads provide the match. Dual Synchronous Queues (see
     * Scherer, Lea, & Scott
     * http://www.cs.rochester.edu/u/scott/papers/2009_Scherer_CACM_SSQ.pdf)
     * additionally arrange that threads enqueuing unmatched data also
     * block.  Dual Transfer Queues support all of these modes, as
     * dictated by callers.
     *
     * A FIFO dual queue may be implemented using a variation of the
     * Michael & Scott (M&S) lock-free queue algorithm
     * (http://www.cs.rochester.edu/u/scott/papers/1996_PODC_queues.pdf).
     * It maintains two pointer fields, "head", pointing to a
     * (matched) node that in turn points to the first actual
     * (unmatched) queue node (or null if empty); and "tail" that
     * points to the last node on the queue (or again null if
     * empty). For example, here is a possible queue with four data
     * elements:
     *
     *  head                tail
     *    |                   |
     *    v                   v
     *    M -> U -> U -> U -> U
     *
     * The M&S queue algorithm is known to be prone to scalability and
     * overhead limitations when maintaining (via CAS) these head and
     * tail pointers. This has led to the development of
     * contention-reducing variants such as elimination arrays (see
     * Moir et al http://portal.acm.org/citation.cfm?id=1074013) and
     * optimistic back pointers (see Ladan-Mozes & Shavit
     * http://people.csail.mit.edu/edya/publications/OptimisticFIFOQueue-journal.pdf).
     * However, the nature of dual queues enables a simpler tactic for
     * improving M&S-style implementations when dual-ness is needed.
     *
     * In a dual queue, each node must atomically maintain its match
     * status. While there are other possible variants, we implement
     * this here as: for a data-mode node, matching entails CASing an
     * "item" field from a non-null data value to null upon match, and
     * vice-versa for request nodes, CASing from null to a data
     * value. (Note that the linearization properties of this style of
     * queue are easy to verify -- elements are made available by
     * linking, and unavailable by matching.) Compared to plain M&S
     * queues, this property of dual queues requires one additional
     * successful atomic operation per enq/deq pair. But it also
     * enables lower cost variants of queue maintenance mechanics. (A
     * variation of this idea applies even for non-dual queues that
     * support deletion of interior elements, such as
     * j.u.c.ConcurrentLinkedQueue.)
     *
     * Once a node is matched, its match status can never again
     * change.  We may thus arrange that the linked list of them
     * contain a prefix of zero or more matched nodes, followed by a
     * suffix of zero or more unmatched nodes. (Note that we allow
     * both the prefix and suffix to be zero length, which in turn
     * means that we do not use a dummy header.)  If we were not
     * concerned with either time or space efficiency, we could
     * correctly perform enqueue and dequeue operations by traversing
     * from a pointer to the initial node; CASing the item of the
     * first unmatched node on match and CASing the next field of the
     * trailing node on appends. (Plus some special-casing when
     * initially empty).  While this would be a terrible idea in
     * itself, it does have the benefit of not requiring ANY atomic
     * updates on head/tail fields.
     *
     * We introduce here an approach that lies between the extremes of
     * never versus always updating queue (head and tail) pointers.
     * This offers a tradeoff between sometimes requiring extra
     * traversal steps to locate the first and/or last unmatched
     * nodes, versus the reduced overhead and contention of fewer
     * updates to queue pointers. For example, a possible snapshot of
     * a queue is:
     *
     *  head           tail
     *    |              |
     *    v              v
     *    M -> M -> U -> U -> U -> U
     *
     * The best value for this "slack" (the targeted maximum distance
     * between the value of "head" and the first unmatched node, and
     * similarly for "tail") is an empirical matter. We have found
     * that using very small constants in the range of 1-3 work best
     * over a range of platforms. Larger values introduce increasing
     * costs of cache misses and risks of long traversal chains, while
     * smaller values increase CAS contention and overhead.
     *
     * Dual queues with slack differ from plain M&S dual queues by
     * virtue of only sometimes updating head or tail pointers when
     * matching, appending, or even traversing nodes; in order to
     * maintain a targeted slack.  The idea of "sometimes" may be
     * operationalized in several ways. The simplest is to use a
     * per-operation counter incremented on each traversal step, and
     * to try (via CAS) to update the associated queue pointer
     * whenever the count exceeds a threshold. Another, that requires
     * more overhead, is to use random number generators to update
     * with a given probability per traversal step.
     *
     * In any strategy along these lines, because CASes updating
     * fields may fail, the actual slack may exceed targeted
     * slack. However, they may be retried at any time to maintain
     * targets.  Even when using very small slack values, this
     * approach works well for dual queues because it allows all
     * operations up to the point of matching or appending an item
     * (hence potentially allowing progress by another thread) to be
     * read-only, thus not introducing any further contention. As
     * described below, we implement this by performing slack
     * maintenance retries only after these points.
     *
     * As an accompaniment to such techniques, traversal overhead can
     * be further reduced without increasing contention of head
     * pointer updates: Threads may sometimes shortcut the "next" link
     * path from the current "head" node to be closer to the currently
     * known first unmatched node, and similarly for tail. Again, this
     * may be triggered with using thresholds or randomization.
     *
     * These ideas must be further extended to avoid unbounded amounts
     * of costly-to-reclaim garbage caused by the sequential "next"
     * links of nodes starting at old forgotten head nodes: As first
     * described in detail by Boehm
     * (http://portal.acm.org/citation.cfm?doid=503272.503282) if a GC
     * delays noticing that any arbitrarily old node has become
     * garbage, all newer dead nodes will also be unreclaimed.
     * (Similar issues arise in non-GC environments.)  To cope with
     * this in our implementation, upon CASing to advance the head
     * pointer, we set the "next" link of the previous head to point
     * only to itself; thus limiting the length of connected dead lists.
     * (We also take similar care to wipe out possibly garbage
     * retaining values held in other Node fields.)  However, doing so
     * adds some further complexity to traversal: If any "next"
     * pointer links to itself, it indicates that the current thread
     * has lagged behind a head-update, and so the traversal must
     * continue from the "head".  Traversals trying to find the
     * current tail starting from "tail" may also encounter
     * self-links, in which case they also continue at "head".
     *
     * It is tempting in slack-based scheme to not even use CAS for
     * updates (similarly to Ladan-Mozes & Shavit). However, this
     * cannot be done for head updates under the above link-forgetting
     * mechanics because an update may leave head at a detached node.
     * And while direct writes are possible for tail updates, they
     * increase the risk of long retraversals, and hence long garbage
     * chains, which can be much more costly than is worthwhile
     * considering that the cost difference of performing a CAS vs
     * write is smaller when they are not triggered on each operation
     * (especially considering that writes and CASes equally require
     * additional GC bookkeeping ("write barriers") that are sometimes
     * more costly than the writes themselves because of contention).
     *
     * Removal of interior nodes (due to timed out or interrupted
     * waits, or calls to remove(x) or Iterator.remove) can use a
     * scheme roughly similar to that described in Scherer, Lea, and
     * Scott's SynchronousQueue. Given a predecessor, we can unsplice
     * any node except the (actual) tail of the queue. To avoid
     * build-up of cancelled trailing nodes, upon a request to remove
     * a trailing node, it is placed in field "cleanMe" to be
     * unspliced upon the next call to unsplice any other node.
     * Situations needing such mechanics are not common but do occur
     * in practice; for example when an unbounded series of short
     * timed calls to poll repeatedly time out but never otherwise
     * fall off the list because of an untimed call to take at the
     * front of the queue. Note that maintaining field cleanMe does
     * not otherwise much impact garbage retention even if never
     * cleared by some other call because the held node will
     * eventually either directly or indirectly lead to a self-link
     * once off the list.
     *
     * *** Overview of implementation ***
     *
     * We use a threshold-based approach to updates, with a slack
     * threshold of two -- that is, we update head/tail when the
     * current pointer appears to be two or more steps away from the
     * first/last node. The slack value is hard-wired: a path greater
     * than one is naturally implemented by checking equality of
     * traversal pointers except when the list has only one element,
     * in which case we keep slack threshold at one. Avoiding tracking
     * explicit counts across method calls slightly simplifies an
     * already-messy implementation. Using randomization would
     * probably work better if there were a low-quality dirt-cheap
     * per-thread one available, but even ThreadLocalRandom is too
     * heavy for these purposes.
     *
     * With such a small slack threshold value, it is rarely
     * worthwhile to augment this with path short-circuiting; i.e.,
     * unsplicing nodes between head and the first unmatched node, or
     * similarly for tail, rather than advancing head or tail
     * proper. However, it is used (in awaitMatch) immediately before
     * a waiting thread starts to block, as a final bit of helping at
     * a point when contention with others is extremely unlikely
     * (since if other threads that could release it are operating,
     * then the current thread wouldn't be blocking).
     *
     * We allow both the head and tail fields to be null before any
     * nodes are enqueued; initializing upon first append.  This
     * simplifies some other logic, as well as providing more
     * efficient explicit control paths instead of letting JVMs insert
     * implicit NullPointerExceptions when they are null.  While not
     * currently fully implemented, we also leave open the possibility
     * of re-nulling these fields when empty (which is complicated to
     * arrange, for little benefit.)
     *
     * All enqueue/dequeue operations are handled by the single method
     * "xfer" with parameters indicating whether to act as some form
     * of offer, put, poll, take, or transfer (each possibly with
     * timeout). The relative complexity of using one monolithic
     * method outweighs the code bulk and maintenance problems of
     * using separate methods for each case.
     *
     * Operation consists of up to three phases. The first is
     * implemented within method xfer, the second in tryAppend, and
     * the third in method awaitMatch.
     *
     * 1. Try to match an existing node
     *
     *    Starting at head, skip already-matched nodes until finding
     *    an unmatched node of opposite mode, if one exists, in which
     *    case matching it and returning, also if necessary updating
     *    head to one past the matched node (or the node itself if the
     *    list has no other unmatched nodes). If the CAS misses, then
     *    a loop retries advancing head by two steps until either
     *    success or the slack is at most two. By requiring that each
     *    attempt advances head by two (if applicable), we ensure that
     *    the slack does not grow without bound. Traversals also check
     *    if the initial head is now off-list, in which case they
     *    start at the new head.
     *
     *    If no candidates are found and the call was untimed
     *    poll/offer, (argument "how" is NOW) return.
     *
     * 2. Try to append a new node (method tryAppend)
     *
     *    Starting at current tail pointer, find the actual last node
     *    and try to append a new node (or if head was null, establish
     *    the first node). Nodes can be appended only if their
     *    predecessors are either already matched or are of the same
     *    mode. If we detect otherwise, then a new node with opposite
     *    mode must have been appended during traversal, so we must
     *    restart at phase 1. The traversal and update steps are
     *    otherwise similar to phase 1: Retrying upon CAS misses and
     *    checking for staleness.  In particular, if a self-link is
     *    encountered, then we can safely jump to a node on the list
     *    by continuing the traversal at current head.
     *
     *    On successful append, if the call was ASYNC, return.
     *
     * 3. Await match or cancellation (method awaitMatch)
     *
     *    Wait for another thread to match node; instead cancelling if
     *    the current thread was interrupted or the wait timed out. On
     *    multiprocessors, we use front-of-queue spinning: If a node
     *    appears to be the first unmatched node in the queue, it
     *    spins a bit before blocking. In either case, before blocking
     *    it tries to unsplice any nodes between the current "head"
     *    and the first unmatched node.
     *
     *    Front-of-queue spinning vastly improves performance of
     *    heavily contended queues. And so long as it is relatively
     *    brief and "quiet", spinning does not much impact performance
     *    of less-contended queues.  During spins threads check their
     *    interrupt status and generate a thread-local random number
     *    to decide to occasionally perform a Thread.yield. While
     *    yield has underdefined specs, we assume that might it help,
     *    and will not hurt in limiting impact of spinning on busy
     *    systems.  We also use smaller (1/2) spins for nodes that are
     *    not known to be front but whose predecessors have not
     *    blocked -- these "chained" spins avoid artifacts of
     *    front-of-queue rules which otherwise lead to alternating
     *    nodes spinning vs blocking. Further, front threads that
     *    represent phase changes (from data to request node or vice
     *    versa) compared to their predecessors receive additional
     *    chained spins, reflecting longer paths typically required to
     *    unblock threads during phase changes.
     */

    /** True if on multiprocessor */
    private static final boolean MP =
        Runtime.getRuntime().availableProcessors() > 1;

    /**
     * The number of times to spin (with randomly interspersed calls
     * to Thread.yield) on multiprocessor before blocking when a node
     * is apparently the first waiter in the queue.  See above for
     * explanation. Must be a power of two. The value is empirically
     * derived -- it works pretty well across a variety of processors,
     * numbers of CPUs, and OSes.
     */
    private static final int FRONT_SPINS   = 1 << 7;

    /**
     * The number of times to spin before blocking when a node is
     * preceded by another node that is apparently spinning.  Also
     * serves as an increment to FRONT_SPINS on phase changes, and as
     * base average frequency for yielding during spins. Must be a
     * power of two.
     */
    private static final int CHAINED_SPINS = FRONT_SPINS >>> 1;

    /**
     * Queue nodes. Uses Object, not E, for items to allow forgetting
     * them after use.  Relies heavily on Unsafe mechanics to minimize
     * unnecessary ordering constraints: Writes that intrinsically
     * precede or follow CASes use simple relaxed forms.  Other
     * cleanups use releasing/lazy writes.
     */
    static final class Node {
        final boolean isData;   // false if this is a request node
        volatile Object item;   // initially non-null if isData; CASed to match
        volatile Node next;
        volatile Thread waiter; // null until waiting

        // CAS methods for fields
        final boolean casNext(Node cmp, Node val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        final boolean casItem(Object cmp, Object val) {
            // assert cmp == null || cmp.getClass() != Node.class;
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        /**
         * Creates a new node. Uses relaxed write because item can only
         * be seen if followed by CAS.
         */
        Node(Object item, boolean isData) {
            UNSAFE.putObject(this, itemOffset, item); // relaxed write
            this.isData = isData;
        }

        /**
         * Links node to itself to avoid garbage retention.  Called
         * only after CASing head field, so uses relaxed write.
         */
        final void forgetNext() {
            UNSAFE.putObject(this, nextOffset, this);
        }

        /**
         * Sets item to self (using a releasing/lazy write) and waiter
         * to null, to avoid garbage retention after extracting or
         * cancelling.
         */
        final void forgetContents() {
            UNSAFE.putOrderedObject(this, itemOffset, this);
            UNSAFE.putOrderedObject(this, waiterOffset, null);
        }

        /**
         * Returns true if this node has been matched, including the
         * case of artificial matches due to cancellation.
         */
        final boolean isMatched() {
            Object x = item;
            // x == this 表示如果是取消的节点，也当作已经匹配
            // （x == null） == isData 表示x有没有值和isData是相反，表示已经被匹配，如果x是null，那么x== null 就是true，但是isData是false就说明，x被更改
            // 什么时候x被更改，就是当前节点被匹配的时候
            return (x == this) || ((x == null) == isData);
        }

        /**
         * Returns true if this is an unmatched request node.
         */
        final boolean isUnmatchedRequest() {
            return !isData && item == null;
        }

        /**
         * Returns true if a node with the given mode cannot be
         * appended to this node because this node is unmatched and
         * has opposite data mode.
         */
        final boolean cannotPrecede(boolean haveData) {
            boolean d = isData;
            Object x;
            return d != haveData && (x = item) != this && (x != null) == d;
        }

        /**
         * Tries to artificially match a data node -- used by remove.
         */
        final boolean tryMatchData() {
            // assert isData;
            Object x = item;
            if (x != null && x != this && casItem(x, null)) {
                LockSupport.unpark(waiter);
                return true;
            }
            return false;
        }

        // Unsafe mechanics
        private static final sun.misc.Unsafe UNSAFE = sun.misc.Unsafe.getUnsafe();
        private static final long nextOffset =
            objectFieldOffset(UNSAFE, "next", Node.class);
        private static final long itemOffset =
            objectFieldOffset(UNSAFE, "item", Node.class);
        private static final long waiterOffset =
            objectFieldOffset(UNSAFE, "waiter", Node.class);

        private static final long serialVersionUID = -3375979862319811754L;
    }

    /** head of the queue; null until first enqueue */
    transient volatile Node head;

    /** predecessor of dangling unspliceable node */
    private transient volatile Node cleanMe; // decl here reduces contention

    /** tail of the queue; null until first append */
    private transient volatile Node tail;

    // CAS methods for fields
    private boolean casTail(Node cmp, Node val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    private boolean casHead(Node cmp, Node val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private boolean casCleanMe(Node cmp, Node val) {
        return UNSAFE.compareAndSwapObject(this, cleanMeOffset, cmp, val);
    }

    /*
     * Possible values for "how" argument in xfer method.
     */
    private static final int NOW   = 0; // for untimed poll, tryTransfer
    private static final int ASYNC = 1; // for offer, put, add
    private static final int SYNC  = 2; // for transfer, take
    private static final int TIMED = 3; // for timed poll, tryTransfer

    @SuppressWarnings("unchecked")
    static <E> E cast(Object item) {
        // assert item == null || item.getClass() != Node.class;
        return (E) item;
    }

    /**
     * Implements all queuing methods. See above for explanation.
     *
     * @param e the item or null for take
     * @param haveData true if this is a put, else a take
     * @param how NOW, ASYNC, SYNC, or TIMED
     * @param nanos timeout in nanosecs, used only if mode is TIMED
     * @return an item if matched, else e
     * @throws NullPointerException if haveData mode but e is null
     */
    private E xfer(E e, boolean haveData, int how, long nanos) {
        if (haveData && (e == null))
            throw new NullPointerException();
        Node s = null;                        // the node to append, if needed

        retry: for (;;) {                     // restart on append race

            for (Node h = head, p = h; p != null;) { // find & match first node
                boolean isData = p.isData;
                Object item = p.item;
                // item != p 表示该节点没有被取消，只有被取消时，item才会设置为item所属的节点
                // (item != null) 表示元素不为空，(item != null ) == isData 这个语句表示元素不为空应该和isData属性一致
                // item 如果为null isData为false，代表获取节点；item如果不为null，isData为true，代表放节点。正常情况是一致的
                // 那么什么时候不一致呢，当获取节点线程取到队列中某个元素时，就会将该元素替换为null（或者阻塞放元素的时候，尝试将被阻塞的线程节点中放入数据，由null变成有数据），表示此元素已经被某个线程获取了
                // 所以这句语句后面的unmatched表示，不管是取节点还是放节点，只要满足该语句，就表示该节点还在队列中，并没没有得到或者被线程取走数据
                if (item != p && (item != null) == isData) { // unmatched
                    if (isData == haveData)   // can't match
                        // isData是不会变的，因此这句话的意思是当前线程是在做和头节点一样的事情，比如取数据和放数据，为什么这里要从头节点开始比较呢
                        // 因为只有相同的动作行为才会进行排队
                        break;
                    if (p.casItem(item, e)) { // match
                        // 这里需要分为俩种情况来说：
                        // 第一种：已经放入元素，当前线程是获取元素的，那么e就是null，因此这句话的意思就是将有值的p节点的item引用设置为null，表示已经被match
                        // 第二种：已经有一个线程在阻塞获取元素，那么e就是有元素的，因此这句话的意思就是将无值的p节点的item引用设置为e，表示无值的p节点已经被match
                        for (Node q = p; q != h;) {
                            // 什么情况下会有多个节点?
                            // 以队列中都是放入的数据，等待线程来阻塞取的视角来看
                            // head head.next都有数据，此时三个线程都来同时取，都进入到casItem节点，最终只有一个线程拿到head数据，此时剩下俩个线程来争抢剩下一个元素
                            // 如果线程B争抢到了后，head.next不等于head。就会存在进入此循环。会将b线程获取到的元素设置为头节点。因为head节点元素被获取了，可以出队
                            Node n = q.next;  // update head by 2
                            if (n != null)    // unless singleton
                                // q也就是p的next节点不为null，那么就将n赋予q
                                q = n;
                            if (head == h && casHead(h, q)) {
                                // 如果h等于head节点，并且成功将q节点设置为head节点
                                // 为什么要这么做，因为p节点已经match，他不需要存在在队列中，因此可以直接出局，将p的next节点设置为头节点。
                                h.forgetNext();
                                // 移除head节点对next节点的引用，这里可以加速垃圾回收。并不是每个节点的next节点都能正确移除，比如此时的p节点
                                break;
                            }
                            // 此处较为复杂涉及到上述俩种情况
                            // head != h 以及caseHead失败俩种情况，都表示一瞬间，head被别人修改了，后面的节点变成head了，此时需要重新判断是否需要retry
                            // 以下三个只要有一个为true就可以无需retry
                            // 1：head为null，表示队列为空，不需要重试
                            // 2：head不为空，但是head的next节点为null，此时也不需要重试，因为有一个元素可能正在排队
                            // 3：head不为空，切head的next也不为空，并且head的next没有被匹配，也不需要重试，那么很明显在排队
                            // 所以只有head的next已经匹配上，才需要重试，因为head的next已经匹配上代表head也已经匹配上，需要清除head节点，防止无效节点滥用
                            // advance and retry
                            if ((h = head)   == null ||
                                (q = h.next) == null || !q.isMatched())
                                break;        // unless slack < 2
                        }
                        // 唤醒p节点
                        LockSupport.unpark(p.waiter);
                        return this.<E>cast(item);
                    }
                } // matched
                // 如果n等于p，表示该元素已经出队，
                // 线程C此时拿到的p还是初始head，此时初始head已经被线程b移除队列（next指向自身）。
                Node n = p.next;
                p = (p != n) ? n : (h = head); // Use head if p offlist
            }

            if (how != NOW) {                 // No matches available
                if (s == null)
                    s = new Node(e, haveData);
                Node pred = tryAppend(s, haveData);
                // 如果前置节点为null，表示mode不一致，需要进行match
                if (pred == null)
                    continue retry;           // lost race vs opposite mode
                if (how != ASYNC)
                    // 超时等待或者永久等待
                    return awaitMatch(s, pred, e, (how == TIMED), nanos);
            }
            return e; // not waiting
        }
    }

    /**
     * Tries to append node s as tail.
     *
     * @param s the node to append
     * @param haveData true if appending in data mode
     * @return null on failure due to losing race with append in
     * different mode, else s's predecessor, or s itself if no
     * predecessor
     */
    private Node tryAppend(Node s, boolean haveData) {
        // t是末尾节点，p是t节点
        for (Node t = tail, p = t;;) {        // move p to last node and append
            Node n, u;                        // temps for reads of next & tail
            // 如果p也就是末尾节点为null，就将p设置为head节点，并且判断是否为null
            if (p == null && (p = head) == null) {
                // 如果head也为null，则将s设置为head节点，表示当前列表是空列表
                // p 和head都为null，表示空节点，直接设置头节点
                if (casHead(null, s))
                    // 设置成功则表示初始化队列成功
                    return s;                 // initialize
            }
            else if (p.cannotPrecede(haveData))
                // 设置头节点失败，先判断是否能够入队，入队的标准是不同mode的节点
                return null;                  // lost race vs opposite mode
            else if ((n = p.next) != null)    // not last; keep traversing
                p = p != t && t != (u = tail) ? (t = u) : // stale tail
                    (p != n) ? n : null;      // restart if off list
            // 这里分为俩种情况
            // 1. 如果p是从tail节点开始的，那么这里就是找到真实的尾节点node.next == null的node（三元表达式后面的）
            // 2. 如果p是从head节点开始的，那么这里就是找到tail节点（三元表达式前面的）
            // 3. p == null 表示尾部节点已经出队，需要从head开始遍历
            else if (!p.casNext(null, s))
                // 入队的首要条件是p的next为null。否则往下面继续遍历
                p = p.next;                   // re-read on CAS failure
            else {
                if (p != t) {                 // update if slack now >= 2
                    // 这里的t一般指代tail节点，而p节点为真实的尾节点，p != t表示真实尾节点不在tail上，需要进行设置尾节点
                    while ((tail != t || !casTail(t, s)) &&
                           (t = tail)   != null &&
                           (s = t.next) != null && // advance and retry
                           (s = s.next) != null && s != t);
                    // 全部为true表示需要进行死循环设置tail节点，为false表示无需设置，表述需要理解
                    // (tail != t || !casTail(t, s)) 表示t不等于tail节点或者设置tai失败（整体为true）需要进行重试，如果tail !=t 为true也就是t不等于tail直接判断后面的，如果为false，则表示相等
                    // 如果!casTail为true，则表示cas失败，需要重试
                    // (t = tail)   != null tail不为null，不是空节点或者只有一个节点
                    // (s = t.next) != null 这里因为s已经入队了，已经不需要了，所以将t.next赋值给s并且不为null表示tail后续有节点存在
                    // (s = s.next) != null && s != t 如果 s = s.next != null 表示s的next如果一旦为null就不需要进行设置tail节点，这里体现了slack>=2的特性
                    // 至于s!=t则表示可以理解为如果s==t ，那么s.next == t == tail 可以想象 tail -> tail.next == s -> s.next == s -> tail
                    // 可以理解为tail节点已经出队了，无需重新设置tail
                }
                // 返回前置节点
                return p;
            }
        }
    }

    /**
     * Spins/yields/blocks until node s is matched or caller gives up.
     *
     * @param s the waiting node
     * @param pred the predecessor of s, or s itself if it has no
     * predecessor, or null if unknown (the null case does not occur
     * in any current calls but may in possible future extensions)
     * @param e the comparison value for checking match
     * @param timed if true, wait only until timeout elapses
     * @param nanos timeout in nanosecs, used only if timed is true
     * @return matched item, or e if unmatched on interrupt or timeout
     */
    private E awaitMatch(Node s, Node pred, E e, boolean timed, long nanos) {
        // 如果是超时等待的话，就计算出纳秒时间
        long lastTime = timed ? System.nanoTime() : 0L;
        Thread w = Thread.currentThread();
        int spins = -1; // initialized after first item and cancel checks
        ThreadLocalRandom randomYields = null; // bound if needed

        for (;;) {
            Object item = s.item;
            if (item != e) {                  // matched
                // item不等于e表示已经获得match（唤醒）
                // assert item != s;
                // 唤醒第一件事就是清除自身引用，加速垃圾回收
                s.forgetContents();           // avoid garbage
                return this.<E>cast(item);
            }
            if ((w.isInterrupted() || (timed && nanos <= 0)) &&
                    s.casItem(e, s)) {       // cancel
                // 如果节点已经被中断或者已经超时则直接取消节点(item等于node自身表示被取消)
                unsplice(pred, s);
                return e;
            }

            // spins默认为-1
            if (spins < 0) {                  // establish spins at/near front
                if ((spins = spinsFor(pred, s.isData)) > 0)
                    // 一般多核情况下，spins都是大于0de
                    randomYields = ThreadLocalRandom.current();
            }
            else if (spins > 0) {             // spin
                if (--spins == 0)
                    // 当最后一次自旋，尝试调整head的path
                    shortenHeadPath();        // reduce slack before blocking
                else if (randomYields.nextInt(CHAINED_SPINS) == 0)
                    // 释放cpu资源，变成就绪状态，等待被调用，每次当前线程随机值等于0时，触发yield。
                    Thread.yield();           // occasionally yield
            }
            else if (s.waiter == null) {
                // 如果当前节点没有线程waiter就进行设置
                s.waiter = w;                 // request unpark then recheck
            }
            else if (timed) {
                // 超时等待
                long now = System.nanoTime();
                if ((nanos -= now - lastTime) > 0)
                    LockSupport.parkNanos(this, nanos);
                lastTime = now;
            }
            else {
                // 完全等待
                LockSupport.park(this);
                s.waiter = null;
                // 万一是虚假唤醒，需要重新进行初始化循环
                spins = -1;                   // spin if front upon wakeup
            }
        }
    }

    /**
     * Returns spin/yield value for a node with given predecessor and
     * data mode. See above for explanation.
     */
    private static int spinsFor(Node pred, boolean haveData) {
        // MP代表多核 ，pred在这里不会为null。这里需要计算出当前节点自旋的次数，基于pred节点来计算
        if (MP && pred != null) {
            // 如果前节点mode和当前不一致，理论上说明pred已经被match，当前节点是队首
            if (pred.isData != haveData)      // phase change
                return FRONT_SPINS + CHAINED_SPINS;
            if (pred.isMatched())             // probably at front
                // 前提是pred的mode和当前相等，并且pred已经被match
                return FRONT_SPINS;
            if (pred.waiter == null)          // pred apparently spinning
                // pred的mode和当前一致，并且没有被match，还在自旋中
                return CHAINED_SPINS;
        }
        return 0;
    }

    /**
     * Tries (once) to unsplice nodes between head and first unmatched
     * or trailing node; failing on contention.
     */
    private void shortenHeadPath() {
        // 什么时候需要触发缩短head的path，首要条件就是head已经被匹配，并且head的next不等于null
        // head 节点要不是没有被match（那么它的next也不会被match），要不已经是matched（next可能被matched，也可能未被matched），
        // 当已经是matched的情况下，它的next节点如果是matched。必然会casHead，替换head，并且将head移除队列（next指向自己）
        Node h, hn, p, q;
        // 如果h还在队列中并且，h已经被match。next不为null
        if ((p = h = head) != null && h.isMatched() &&
            (q = hn = h.next) != null) {
            Node n;
            // 如果q=h.next等于它自己的next，此时代表q和h可能都已经出队，或者h出队
            while ((n = q.next) != q) {
                // 如果q的next等于null（也就是n，代表q是尾节点）并且q没有被matched（q后面有在排队的）
                if (n == null || !q.isMatched()) {
                   // 第一次进来如果满足说明q是head后面节点，并且q的next为null，此时直接break
                    // 只有第二次进来，hn!=q 说明成立，并且h.next == hn说明没有人此时更改过h.next节点，可以调整h的next节点
                    if (hn != q && h.next == hn)
                        h.casNext(hn, q);
                    break;
                }
                // 往后进行遍历
                p = q;
                q = n;
            }
            // q和h都出队，就跳出循环，忽略
        }
    }

    /* -------------- Traversal methods -------------- */

    /**
     * Returns the successor of p, or the head node if p.next has been
     * linked to self, which will only be true if traversing with a
     * stale pointer that is now off the list.
     */
    final Node succ(Node p) {
        // 如果p已经不在队里中，就返回head，如果在队列中就返回p的next节点
        Node next = p.next;
        return (p == next) ? head : next;
    }

    /**
     * Returns the first unmatched node of the given mode, or null if
     * none.  Used by methods isEmpty, hasWaitingConsumer.
     */
    private Node firstOfMode(boolean isData) {
        // isData表示是否是有数据的，true表示有数据，false表示没数据
        for (Node p = head; p != null; p = succ(p)) {
            if (!p.isMatched())
                // 如果p的节点isData和入参一致，就返回p。否则返回null
                return (p.isData == isData) ? p : null;
        }
        // 没有找到也返回null
        return null;
    }

    /**
     * Returns the item in the first unmatched node with isData; or
     * null if none.  Used by peek.
     */
    private E firstDataItem() {
        for (Node p = head; p != null; p = succ(p)) {
            Object item = p.item;
            if (p.isData) {
                if (item != null && item != p)
                    return this.<E>cast(item);
            }
            else if (item == null)
                return null;
        }
        return null;
    }

    /**
     * Traverses and counts unmatched nodes of the given mode.
     * Used by methods size and getWaitingConsumerCount.
     */
    private int countOfMode(boolean data) {
        int count = 0;
        for (Node p = head; p != null; ) {
            if (!p.isMatched()) {
                if (p.isData != data)
                    return 0;
                if (++count == Integer.MAX_VALUE) // saturated
                    break;
            }
            Node n = p.next;
            if (n != p)
                p = n;
            else {
                count = 0;
                p = head;
            }
        }
        return count;
    }

    final class Itr implements Iterator<E> {
        private Node nextNode;   // next node to return item for
        private E nextItem;      // the corresponding item
        private Node lastRet;    // last returned node, to support remove
        private Node lastPred;   // predecessor to unlink lastRet

        /**
         * Moves to next node after prev, or first node if prev null.
         */
        private void advance(Node prev) {
            lastPred = lastRet;
            lastRet = prev;
            for (Node p = (prev == null) ? head : succ(prev);
                 p != null; p = succ(p)) {
                Object item = p.item;
                if (p.isData) {
                    if (item != null && item != p) {
                        nextItem = LinkedTransferQueue.this.<E>cast(item);
                        nextNode = p;
                        return;
                    }
                }
                else if (item == null)
                    break;
            }
            nextNode = null;
        }

        Itr() {
            advance(null);
        }

        public final boolean hasNext() {
            return nextNode != null;
        }

        public final E next() {
            Node p = nextNode;
            if (p == null) throw new NoSuchElementException();
            E e = nextItem;
            advance(p);
            return e;
        }

        public final void remove() {
            Node p = lastRet;
            if (p == null) throw new IllegalStateException();
            findAndRemoveDataNode(lastPred, p);
        }
    }

    /* -------------- Removal methods -------------- */

    /**
     * Unsplices (now or later) the given deleted/cancelled node with
     * the given predecessor.
     *
     * @param pred predecessor of node to be unspliced
     * @param s the node to be unspliced
     */
    private void unsplice(Node pred, Node s) {
        // item设置节点自身
        // waiter设置为null
        s.forgetContents(); // clear unneeded fields
        /*
         * At any given time, exactly one node on list cannot be
         * unlinked -- the last inserted node. To accommodate this, if
         * we cannot unlink s, we save its predecessor as "cleanMe",
         * processing the previously saved version first. Because only
         * one node in the list can have a null next, at least one of
         * node s or the node previously saved can always be
         * processed, so this always terminates.
         */
        // 表示pred还在队列中,并且当前队列中只有一个元素
        if (pred != null && pred != s) {
            // 确保pred.next等于s
            while (pred.next == s) {
                Node oldpred = (cleanMe == null) ? null : reclean();
                // s为cancel，s.next不为null。将pred.next从s设置为n
                Node n = s.next;
                if (n != null) {
                    if (n != s)
                        pred.casNext(s, n);
                    break;
                }
                // 如果s为最后节点，则将s的前置节点pred保存起来
                if (oldpred == pred ||      // Already saved
                    ((oldpred == null || oldpred.next == s) &&
                     casCleanMe(oldpred, pred))) {
                    break;
                }
            }
        }
    }

    /**
     * Tries to unsplice the deleted/cancelled node held in cleanMe
     * that was previously uncleanable because it was at tail.
     *
     * @return current cleanMe node (or null)
     */
    private Node reclean() {
        /*
         * cleanMe is, or at one time was, predecessor of a cancelled
         * node s that was the tail so could not be unspliced.  If it
         * is no longer the tail, try to unsplice if necessary and
         * make cleanMe slot available.  This differs from similar
         * code in unsplice() because we must check that pred still
         * points to a matched node that can be unspliced -- if not,
         * we can (must) clear cleanMe without unsplicing.  This can
         * loop only due to contention.
         */
        Node pred;
        while ((pred = cleanMe) != null) {
            Node s = pred.next;
            Node n;
            if (s == null || s == pred || !s.isMatched())
                casCleanMe(pred, null); // already gone
            else if ((n = s.next) != null) {
                if (n != s)
                    pred.casNext(s, n);
                casCleanMe(pred, null);
            }
            else
                break;
        }
        return pred;
    }

    /**
     * Main implementation of Iterator.remove(). Finds
     * and unsplices the given data node.
     *
     * @param possiblePred possible predecessor of s
     * @param s the node to remove
     */
    final void findAndRemoveDataNode(Node possiblePred, Node s) {
        // assert s.isData;
        if (s.tryMatchData()) {
            if (possiblePred != null && possiblePred.next == s)
                unsplice(possiblePred, s); // was actual predecessor
            else {
                for (Node pred = null, p = head; p != null; ) {
                    if (p == s) {
                        unsplice(pred, p);
                        break;
                    }
                    if (p.isUnmatchedRequest())
                        break;
                    pred = p;
                    if ((p = p.next) == pred) { // stale
                        pred = null;
                        p = head;
                    }
                }
            }
        }
    }

    /**
     * Main implementation of remove(Object)
     */
    private boolean findAndRemove(Object e) {
        if (e != null) {
            for (Node pred = null, p = head; p != null; ) {
                Object item = p.item;
                if (p.isData) {
                    if (item != null && item != p && e.equals(item) &&
                        p.tryMatchData()) {
                        unsplice(pred, p);
                        return true;
                    }
                }
                else if (item == null)
                    break;
                pred = p;
                if ((p = p.next) == pred) { // stale
                    pred = null;
                    p = head;
                }
            }
        }
        return false;
    }


    /**
     * Creates an initially empty {@code LinkedTransferQueue}.
     */
    public LinkedTransferQueue() {
    }

    /**
     * Creates a {@code LinkedTransferQueue}
     * initially containing the elements of the given collection,
     * added in traversal order of the collection's iterator.
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public LinkedTransferQueue(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never block.
     *
     * @throws NullPointerException if the specified element is null
     */
    public void put(E e) {
        xfer(e, true, ASYNC, 0);
    }

    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never block or
     * return {@code false}.
     *
     * @return {@code true} (as specified by
     *  {@link BlockingQueue#offer(Object,long, TimeUnit) BlockingQueue.offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e, long timeout, TimeUnit unit) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never return {@code false}.
     *
     * @return {@code true} (as specified by
     *         {@link BlockingQueue#offer(Object) BlockingQueue.offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never throw
     * {@link IllegalStateException} or return {@code false}.
     *
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    /**
     * Transfers the element to a waiting consumer immediately, if possible.
     *
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long,TimeUnit) poll}),
     * otherwise returning {@code false} without enqueuing the element.
     *
     * @throws NullPointerException if the specified element is null
     */
    public boolean tryTransfer(E e) {
        return xfer(e, true, NOW, 0) == null;
    }

    /**
     * Transfers the element to a consumer, waiting if necessary to do so.
     *
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long,TimeUnit) poll}),
     * else inserts the specified element at the tail of this queue
     * and waits until the element is received by a consumer.
     *
     * @throws NullPointerException if the specified element is null
     */
    public void transfer(E e) throws InterruptedException {
        if (xfer(e, true, SYNC, 0) != null) {
            Thread.interrupted(); // failure possible only due to interrupt
            throw new InterruptedException();
        }
    }

    /**
     * Transfers the element to a consumer if it is possible to do so
     * before the timeout elapses.
     *
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long,TimeUnit) poll}),
     * else inserts the specified element at the tail of this queue
     * and waits until the element is received by a consumer,
     * returning {@code false} if the specified wait time elapses
     * before the element can be transferred.
     *
     * @throws NullPointerException if the specified element is null
     */
    public boolean tryTransfer(E e, long timeout, TimeUnit unit)
        throws InterruptedException {
        if (xfer(e, true, TIMED, unit.toNanos(timeout)) == null)
            return true;
        if (!Thread.interrupted())
            return false;
        throw new InterruptedException();
    }

    public E take() throws InterruptedException {
        E e = xfer(null, false, SYNC, 0);
        if (e != null)
            return e;
        Thread.interrupted();
        throw new InterruptedException();
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = xfer(null, false, TIMED, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted())
            return e;
        throw new InterruptedException();
    }

    public E poll() {
        return xfer(null, false, NOW, 0);
    }

    /**
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        E e;
        while ( (e = poll()) != null) {
            c.add(e);
            ++n;
        }
        return n;
    }

    /**
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        E e;
        while (n < maxElements && (e = poll()) != null) {
            c.add(e);
            ++n;
        }
        return n;
    }

    /**
     * Returns an iterator over the elements in this queue in proper
     * sequence, from head to tail.
     *
     * <p>The returned iterator is a "weakly consistent" iterator that
     * will never throw
     * {@link ConcurrentModificationException ConcurrentModificationException},
     * and guarantees to traverse elements as they existed upon
     * construction of the iterator, and may (but is not guaranteed
     * to) reflect any modifications subsequent to construction.
     *
     * @return an iterator over the elements in this queue in proper sequence
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    public E peek() {
        return firstDataItem();
    }

    /**
     * Returns {@code true} if this queue contains no elements.
     *
     * @return {@code true} if this queue contains no elements
     */
    public boolean isEmpty() {
        return firstOfMode(true) == null;
    }

    public boolean hasWaitingConsumer() {
        return firstOfMode(false) != null;
    }

    /**
     * Returns the number of elements in this queue.  If this queue
     * contains more than {@code Integer.MAX_VALUE} elements, returns
     * {@code Integer.MAX_VALUE}.
     *
     * <p>Beware that, unlike in most collections, this method is
     * <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of these queues, determining the current
     * number of elements requires an O(n) traversal.
     *
     * @return the number of elements in this queue
     */
    public int size() {
        return countOfMode(true);
    }

    public int getWaitingConsumerCount() {
        return countOfMode(false);
    }

    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element {@code e} such
     * that {@code o.equals(e)}, if this queue contains one or more such
     * elements.
     * Returns {@code true} if this queue contained the specified element
     * (or equivalently, if this queue changed as a result of the call).
     *
     * @param o element to be removed from this queue, if present
     * @return {@code true} if this queue changed as a result of the call
     */
    public boolean remove(Object o) {
        return findAndRemove(o);
    }

    /**
     * Always returns {@code Integer.MAX_VALUE} because a
     * {@code LinkedTransferQueue} is not capacity constrained.
     *
     * @return {@code Integer.MAX_VALUE} (as specified by
     *         {@link BlockingQueue#remainingCapacity()})
     */
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    /**
     * Saves the state to a stream (that is, serializes it).
     *
     * @serialData All of the elements (each an {@code E}) in
     * the proper order, followed by a null
     * @param s the stream
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        s.defaultWriteObject();
        for (E e : this)
            s.writeObject(e);
        // Use trailing null as sentinel
        s.writeObject(null);
    }

    /**
     * Reconstitutes the Queue instance from a stream (that is,
     * deserializes it).
     *
     * @param s the stream
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        for (;;) {
            @SuppressWarnings("unchecked") E item = (E) s.readObject();
            if (item == null)
                break;
            else
                offer(item);
        }
    }

    // Unsafe mechanics

    private static final sun.misc.Unsafe UNSAFE = sun.misc.Unsafe.getUnsafe();
    private static final long headOffset =
        objectFieldOffset(UNSAFE, "head", LinkedTransferQueue.class);
    private static final long tailOffset =
        objectFieldOffset(UNSAFE, "tail", LinkedTransferQueue.class);
    private static final long cleanMeOffset =
        objectFieldOffset(UNSAFE, "cleanMe", LinkedTransferQueue.class);

    static long objectFieldOffset(sun.misc.Unsafe UNSAFE,
                                  String field, Class<?> klazz) {
        try {
            return UNSAFE.objectFieldOffset(klazz.getDeclaredField(field));
        } catch (NoSuchFieldException e) {
            // Convert Exception to corresponding Error
            NoSuchFieldError error = new NoSuchFieldError(field);
            error.initCause(e);
            throw error;
        }
    }

}
