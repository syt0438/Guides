import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 并发更新数由可选的 concurrencyLevel 构造参数决定。
 * <p>
 * ConcurrentHashMap，采用分而治之的思想，在内部创建创建多个 segment 分段锁,
 * 通过 segment 的数量来控制并发更新的数量，
 * 允许指定数量（segments 数组长度）的并发更新，而不会产生争用问题，
 * 因为 hash 表中的 put 操作基本上是随机的，所以实际的并发性会有所不同。
 * （segment 理解为：具有与 hashtable 相同语义的 hash 表，在 ConcurrentHashMap 中，
 * 作为其子表意义存在）
 * <p>
 * 分段锁的数量过多，会浪费存储空间和操作时间
 * 分段锁的时间过少，会导致更高概率的线程争用
 * 不过在一个数量级之内，过高或过低，都不会产生明显的影响
 * <p>
 * 一写多读时，segment 的数量为 1 是最为合适的
 * <p>
 * ConcurrentHashMap 不允许使用 null 作为 key 和 value 值
 * <p>
 * 判断两个 key 是否相同
 * 1. 首先判断两个 key 的引用地址是否相同，如果引用地址相同，则判定两个 key 相同
 * 2. 如果引用地址不通过，则判断两个 key 的 hash 值和 equals 方法调用是否相同，
 * 如果 hash 与 equals 调用都相同，则判定两个 key 相同
 *
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/8 14:58
 * @since 1.5
 */
@SuppressWarnings("all")
public class ConcurrentHashMap<K, V> extends AbstractMap<K, V>
        implements ConcurrentMap<K, V>, Serializable {
    private static final long serialVersionUID = 7249069246763182397L;

    /* ---------------- Constants -------------- */

    /**
     * The default initial capacity for this table,
     * used when not otherwise specified in a constructor.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The default load factor for this table, used when not
     * otherwise specified in a constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The default concurrency level for this table, used when not
     * otherwise specified in a constructor.
     */
    static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly
     * specified by either of the constructors with arguments.  MUST
     * be a power of two <= 1<<30 to ensure that entries are indexable
     * using ints.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * segment 分段锁（hash 表）的数据槽数量
     */
    static final int MIN_SEGMENT_TABLE_CAPACITY = 2;

    /**
     * The maximum number of segments to allow; used to bound
     * constructor arguments. Must be power of two less than 1 << 24.
     */
    static final int MAX_SEGMENTS = 1 << 16; // slightly conservative

    /**
     * Number of unsynchronized retries in size and containsValue
     * methods before resorting to locking. This is used to avoid
     * unbounded retries if tables undergo continuous modification
     * which would make it impossible to obtain an accurate result.
     */
    static final int RETRIES_BEFORE_LOCK = 2;

    /* ---------------- Fields -------------- */

    /**
     * Mask value for indexing into segments. The upper bits of a
     * key's hash code are used to choose the segment.
     */
    final int segmentMask;

    /**
     * Shift value for indexing within segments.
     */
    final int segmentShift;

    /**
     * 分段锁 segments 数组, 每一个 segment 都是一个 hash 表
     */
    final Segment<K, V>[] segments;

    transient Set<K> keySet;
    transient Set<Map.Entry<K, V>> entrySet;
    transient Collection<V> values;

    /**
     * ConcurrentHashMap list entry. Note that this is never exported
     * out as a user-visible Map.Entry.
     */
    static final class HashEntry<K, V> {
        final int hash;
        final K key;
        volatile V value;
        volatile HashEntry<K, V> next;

        HashEntry(int hash, K key, V value, HashEntry<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        /**
         * Sets next field with volatile write semantics.  (See above
         * about use of putOrderedObject.)
         */
        final void setNext(HashEntry<K, V> n) {
            UNSAFE.putOrderedObject(this, nextOffset, n);
        }

        // Unsafe mechanics
        static final sun.misc.Unsafe UNSAFE;
        static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class k = HashEntry.class;
                nextOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * 获取指定索引的数据槽（链首节点）
     */
    @SuppressWarnings("unchecked")
    static final <K, V> HashEntry<K, V> entryAt(HashEntry<K, V>[] tab, int i) {
        return (tab == null) ? null :
                (HashEntry<K, V>) UNSAFE.getObjectVolatile
                        (tab, ((long) i << TSHIFT) + TBASE);
    }

    /**
     * 设置 hash 表中，指定索引位置的数据槽（链首节点）
     */
    static final <K, V> void setEntryAt(HashEntry<K, V>[] tab, int i,
                                        HashEntry<K, V> e) {
        UNSAFE.putOrderedObject(tab, ((long) i << TSHIFT) + TBASE, e);
    }

    /**
     * Applies a supplemental hash function to a given hashCode, which
     * defends against poor quality hash functions.  This is critical
     * because ConcurrentHashMap uses power-of-two length hash tables,
     * that otherwise encounter collisions for hashCodes that do not
     * differ in lower or upper bits.
     */
    private static int hash(int h) {
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
        h += (h << 15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h << 3);
        h ^= (h >>> 6);
        h += (h << 2) + (h << 14);
        return h ^ (h >>> 16);
    }

    /**
     * Segment 具有与 Hash 表相同的语义
     * <p>
     * Segment 内部始终了保证数据的一致性，可以在不加锁的情况下进行并发读取。
     * <p>
     * 继承 Lock 目的为简化一些锁操作
     */
    static final class Segment<K, V> extends ReentrantLock implements Serializable {

        private static final long serialVersionUID = 2249069246763182397L;

        /**
         * 自旋锁 CAS 尝试次数的最大阀值
         * <p>
         * 如果可用处理器数量为 1, 则说明实际上无锁竞争，而且有线程独占当前处理器，
         * 因此将自旋锁升级为监视器锁，阻塞等待获取
         */
        static final int MAX_SCAN_RETRIES =
                Runtime.getRuntime().availableProcessors() > 1 ? 64 : 1;

        /**
         * 分段锁 segment 的 hash 表
         */
        transient volatile HashEntry<K, V>[] table;

        /**
         * segment 的 hash 表中元素的数量
         * <p>
         * 在 lock 及 volatile 语义中读取时，保持可见性
         */
        transient int count;

        /**
         * The total number of mutative operations in this segment.
         * Even though this may overflows 32 bits, it provides
         * sufficient accuracy for stability checks in CHM isEmpty()
         * and size() methods.  Accessed only either within locks or
         * among other volatile reads that maintain visibility.
         */
        transient int modCount;

        /**
         * The table is rehashed when its size exceeds this threshold.
         * (The value of this field is always <tt>(int)(capacity *
         * loadFactor)</tt>.)
         */
        transient int threshold;

        /**
         * The load factor for the hash table.  Even though this value
         * is same for all segments, it is replicated to avoid needing
         * links to outer object.
         *
         * @serial
         */
        final float loadFactor;

        Segment(float lf, int threshold, HashEntry<K, V>[] tab) {
            this.loadFactor = lf;
            this.threshold = threshold;
            this.table = tab;
        }

        final V put(K key, int hash, V value, boolean onlyIfAbsent) {
            /*
             * 首先假设当前 segment 为无锁竞争，尝试通过 CAS 方式获取锁，
             * 如果获取锁失败，则说明当前 segment 存在锁竞争。
             *
             * 当存在锁竞争，则升级为自旋锁，通过自旋的方式不断尝试获取锁。
             * 当自旋达到阀值时，将自旋锁升级为监视器锁，阻塞等待获取该锁。
             *
             * 在自旋获取锁的过程中，扫描当前数据槽中所有节点，查看 key
             * 是否已存在，如果 key 不存在，则返回一个新的节点。
             */
            HashEntry<K, V> node = tryLock() ? null : scanAndLockForPut(key, hash, value);
            V oldValue;

            try {
                HashEntry<K, V>[] tab = table;
                int index = (tab.length - 1) & hash;
                // 获取指定索引位置的数据槽（链首节点）
                HashEntry<K, V> first = entryAt(tab, index);

                //#region 再次扫描当前数据槽所有节点，如果 key 已存在，则更新该节点
                // 如果 key 不存在，则通过首插的方式添加新节点
                for (HashEntry<K, V> e = first; ; ) {
                    if (e != null) {
                        K k;

                        /*
                         * 如果 key 已存在，则更新当前节点，并记录当前节点的原有值，
                         */
                        if ((k = e.key) == key || (e.hash == hash && key.equals(k))) {
                            oldValue = e.value;

                            if (!onlyIfAbsent) {
                                e.value = value;
                                ++modCount;
                            }

                            break;
                        }

                        e = e.next;
                    } else {
                        //#region 如果数据槽尚未初始化 或者 如果再次扫描后，key 在数据槽中仍然不存在，则通过首插的方式，添加新节点到数据槽

                        if (node != null) {
                            // 首插操作
                            node.setNext(first);
                        } else {
                            // 创建新节点，并进行首插操作
                            node = new HashEntry<K, V>(hash, key, value, first);
                        }

                        int c = count + 1;

                        // 如果当前 segment 的 count 数量大于阀值 并且数据槽的数量小于阀值，则对 hash 表进行扩容
                        if (c > threshold && tab.length < MAXIMUM_CAPACITY) {
                            rehash(node);
                        } else {
                            // 将当前数据槽的链首节点更新为新节点
                            setEntryAt(tab, index, node);
                        }

                        ++modCount;

                        count = c;
                        oldValue = null;

                        //#endregion

                        break;
                    }
                }
                //#endregion

            } finally {
                unlock();
            }

            return oldValue;
        }

        /**
         * Doubles size of table and repacks entries, also adding the
         * given node to new table
         * 对 hash 表进行扩容，扩容 1 倍
         */
        @SuppressWarnings("unchecked")
        private void rehash(HashEntry<K, V> node) {
            /*
             * 扩容采用二次幂扩展的方式，所以每个元素必须保持相同的索引，或以两个偏移的幂移动。
             *
             * 因为旧节点的 next 节点不会改变，所以可以通过旧节点复用的方式，来消除不必要的节点创建。
             *
             * 统计表明：在默认阀值情况下，当 hash 表扩容时，只有六分之一的节点需要被克隆。
             */
            HashEntry<K, V>[] oldTable = table;
            int oldCapacity = oldTable.length;

            int newCapacity = oldCapacity << 1; // 扩容为 2的 1 次方 即扩容 1 倍
            threshold = (int) (newCapacity * loadFactor); // 计算新的数据槽阀值 默认为 16 << 1 * 0.75 = 24 ---> 32 << 1 * 0.75 = 48

            HashEntry<K, V>[] newTable = (HashEntry<K, V>[]) new HashEntry[newCapacity];

            // hash 表的最大索引
            int sizeMask = newCapacity - 1;

            //#region 遍历原有 hash 表的所有数据槽，重新映射到新的 hash 表中
            for (int i = 0; i < oldCapacity; i++) { // 遍历 原有 hash 表（所有数据槽）
                // 记录原有 hash 表中目标索引位置，对应的数据槽链首节点
                HashEntry<K, V> e = oldTable[i];

                if (e != null) {
                    HashEntry<K, V> next = e.next;
                    int idx = e.hash & sizeMask; // 计算链首节点在 新 hash 表中的索引

                    if (next == null)   //  Single node on list
                        // 如果为单节点链表，直接将链首节点放入新 hash 表中的指定索引位置
                        newTable[idx] = e;
                    else { // Reuse consecutive sequence at same slot
                        // 如果非单节点链表
                        HashEntry<K, V> lastRun = e; // 记录链首节点
                        int lastIdx = idx;          // 记录链首节点在新 hash 表中的索引

                        /*
                         * 从链首节点的 next 节点开始，遍历整个数据槽，将每个节点与链首节点的 hash 表索引位置相比较，
                         * 如果不一致，则将记录 链首节点的索引和对象信息，替换当前节点的索引和对象信息
                         *
                         * 即：记录当前数据槽中，最后一个与链首节点索引不一致的节点信息
                         *
                         */
                        for (HashEntry<K, V> last = next; last != null; last = last.next) {
                            // 计算出当前节点在新 hash 表中的索引
                            int k = last.hash & sizeMask;

                            // 如果链首节点和当前节点的索引不一致，即将记录信息更新为当前节点的信息
                            if (k != lastIdx) {
                                lastIdx = k;
                                lastRun = last;
                            }
                        }

                        // 将记录的索引信息和对象更新到新的 hash 表中，作为新 hash 表中指定位置的数据槽首节点
                        // 即：将当前数据槽中最后的节点，作为新 hash 表中指定位置的数据槽链首节点
                        newTable[lastIdx] = lastRun;

                        /*
                         * 遍历剩余的节点：
                         * 1. 重新计算 hash ，获取当前节点坐落新 hash 表的索引位置
                         * 2. 通过该索引位置获取对应的数据槽链首节点
                         * 3. 通过首插的方式，将该数据槽首节点替换为当前节点
                         */
                        for (HashEntry<K, V> p = e; p != lastRun; p = p.next) {
                            V v = p.value;
                            int h = p.hash;
                            int k = h & sizeMask;               // 计算当前节点在新 hash 表的索引
                            HashEntry<K, V> n = newTable[k];     // 根据上面计算的索引，获取新 hash 表中指定位置的链首节点

                            // 首插方式：将当前节点作为指定索引位置的数据槽链首节点，更新到 hash 表中
                            newTable[k] = new HashEntry<K, V>(h, p.key, v, n);
                        }
                    }
                }
            }
            //#endregion

            //#region 首插方式：计算新节点在新 hash 表中索引位置，将目标索引位置对应数据槽的链首节点，替换为当前节点
            int nodeIndex = node.hash & sizeMask; // add the new node
            node.setNext(newTable[nodeIndex]);
            newTable[nodeIndex] = node;
            //endregion

            // 将原有 hash 表替换为新 hash 表
            table = newTable;
        }

        /**
         * 1.扫描当前数据槽所有节点，查看 key 是已存在：
         * -> 如果已存在，停止扫描当前数据槽，继续执行获取锁操作
         * -> 如果未存在，则创建一个新的节点记录到 node 中，停止扫描当前数据槽，继续执行获取锁操作
         * 2. 获取到锁之后，返回之前记录的 node 节点
         *
         * @return 如果 key 不存在，则创建一个新的节点返回
         */
        private HashEntry<K, V> scanAndLockForPut(K key, int hash, V value) {
            // 根据 hash 值，从指定的 segment 中获取链首节点
            HashEntry<K, V> first = entryForHash(this, hash);
            HashEntry<K, V> e = first;
            HashEntry<K, V> node = null;

            int retries = -1; // negative while locating node

            // CAS 不断获取锁
            while (!tryLock()) {
                HashEntry<K, V> f; // to recheck first below

                /*
                 * 扫描当前数据槽所有节点，查看 key 是已存在：
                 * 如果已存在，停止扫描当前数据槽，继续执行获取锁操作
                 * 如果未存在，则创建一个新的节点记录到 node 中，继续执行获取锁操作
                 */
                if (retries < 0) {
                    if (e == null) {
                        // 如果当前数据槽未构建 或者 key 不存在于当前数据槽中，则创建一个新节点
                        if (node == null) // speculatively create node
                        {
                            node = new HashEntry<K, V>(hash, key, value, null);
                        }

                        retries = 0;
                    } else if (key.equals(e.key))
                        retries = 0;
                    else
                        e = e.next;
                } else if (++retries > MAX_SCAN_RETRIES) {
                    // 当 CAS 自旋次数，达到阀值时，当前线程加入锁池，阻塞等会获取锁
                    lock();

                    break;
                } else if ((retries & 1) == 0 &&
                        (f = entryForHash(this, hash)) != first) {
                    // 如果在获取锁的过程中，当前数据槽被其它线程所修改，则获取新的数据槽，重新遍历
                    e = first = f;

                    retries = -1;
                }
            }

            return node;
        }

        /**
         * Scans for a node containing the given key while trying to
         * acquire lock for a remove or replace operation. Upon
         * return, guarantees that lock is held.  Note that we must
         * lock even if the key is not found, to ensure sequential
         * consistency of updates.
         */
        private void scanAndLock(Object key, int hash) {
            // similar to but simpler than scanAndLockForPut
            HashEntry<K, V> first = entryForHash(this, hash);
            HashEntry<K, V> e = first;
            int retries = -1;
            while (!tryLock()) {
                HashEntry<K, V> f;
                if (retries < 0) {
                    if (e == null || key.equals(e.key))
                        retries = 0;
                    else
                        e = e.next;
                } else if (++retries > MAX_SCAN_RETRIES) {
                    lock();
                    break;
                } else if ((retries & 1) == 0 &&
                        (f = entryForHash(this, hash)) != first) {
                    e = first = f;
                    retries = -1;
                }
            }
        }

        /**
         * Remove; match on key only if value null, else match both.
         */
        final V remove(Object key, int hash, Object value) {
            if (!tryLock())
                scanAndLock(key, hash);
            V oldValue = null;
            try {
                HashEntry<K, V>[] tab = table;
                int index = (tab.length - 1) & hash;
                HashEntry<K, V> e = entryAt(tab, index);
                HashEntry<K, V> pred = null;
                while (e != null) {
                    K k;
                    HashEntry<K, V> next = e.next;
                    if ((k = e.key) == key ||
                            (e.hash == hash && key.equals(k))) {
                        V v = e.value;
                        if (value == null || value == v || value.equals(v)) {
                            if (pred == null)
                                setEntryAt(tab, index, next);
                            else
                                pred.setNext(next);
                            ++modCount;
                            --count;
                            oldValue = v;
                        }
                        break;
                    }
                    pred = e;
                    e = next;
                }
            } finally {
                unlock();
            }
            return oldValue;
        }

        final boolean replace(K key, int hash, V oldValue, V newValue) {
            if (!tryLock())
                scanAndLock(key, hash);
            boolean replaced = false;
            try {
                HashEntry<K, V> e;
                for (e = entryForHash(this, hash); e != null; e = e.next) {
                    K k;
                    if ((k = e.key) == key ||
                            (e.hash == hash && key.equals(k))) {
                        if (oldValue.equals(e.value)) {
                            e.value = newValue;
                            ++modCount;
                            replaced = true;
                        }
                        break;
                    }
                }
            } finally {
                unlock();
            }
            return replaced;
        }

        final V replace(K key, int hash, V value) {
            if (!tryLock())
                scanAndLock(key, hash);
            V oldValue = null;
            try {
                HashEntry<K, V> e;
                for (e = entryForHash(this, hash); e != null; e = e.next) {
                    K k;
                    if ((k = e.key) == key ||
                            (e.hash == hash && key.equals(k))) {
                        oldValue = e.value;
                        e.value = value;
                        ++modCount;
                        break;
                    }
                }
            } finally {
                unlock();
            }
            return oldValue;
        }

        /**
         * 通过加锁以独占的方式，将 segment 中 hash 表的所有数据槽的链首节点置空（help GC）
         * <p>
         * 重置 segment 中 hash 表的元素数量（count = 0）
         */
        final void clear() {
            lock();

            try {
                HashEntry<K, V>[] tab = table;

                for (int i = 0; i < tab.length; i++) {
                    // 将指定索引位置上的数据槽的链首节点置空
                    setEntryAt(tab, i, null);
                }

                ++modCount;
                count = 0;
            } finally {
                unlock();
            }
        }
    }

    // Accessing segments

    /**
     * 使用 Unsafe 通过 volatile 语义，从 segments 数组中获取 segment
     */
    @SuppressWarnings("unchecked")
    static final <K, V> Segment<K, V> segmentAt(Segment<K, V>[] ss, int j) {
        long u = (j << SSHIFT) + SBASE;
        return ss == null ? null :
                (Segment<K, V>) UNSAFE.getObjectVolatile(ss, u);
    }

    /**
     * <p>
     * 返回索引位置的目标 segment，如果 segment 不存在，则使用初始构造的 segment 作为原型，创建一个新的 segment，
     * 创建完成后，通过 CAS 自旋的方式 更新到 segments 中
     * <p>
     *
     * @param k the index
     * @return the segment
     */
    @SuppressWarnings("unchecked")
    private Segment<K, V> ensureSegment(int k) {
        final Segment<K, V>[] ss = this.segments;

        long u = (k << SSHIFT) + SBASE; // raw offset

        Segment<K, V> seg;

        if ((seg = (Segment<K, V>) UNSAFE.getObjectVolatile(ss, u)) == null) {
            // 使用初始构造的 segment 作为原型，创建一个新的 segment
            Segment<K, V> proto = ss[0];

            int cap = proto.table.length;
            float lf = proto.loadFactor;
            int threshold = (int) (cap * lf);

            HashEntry<K, V>[] tab = (HashEntry<K, V>[]) new HashEntry[cap];

            if ((seg = (Segment<K, V>) UNSAFE.getObjectVolatile(ss, u)) == null) { // recheck
                Segment<K, V> s = new Segment<>(lf, threshold, tab);

                /*
                 * 每一次 CAS 更新（将创建的新 segment 更新到 segmetns 中）操作
                 * 之前，都会尝试从主内存中刷新该指定索引位置的 segment，再次检查
                 *
                 * 以此防止在当前线程自旋过程中，已经有其它的线程将指定索引位置的 segment
                 * 创建并更新到 segments 汇总
                 * */
                while ((seg = (Segment<K, V>) UNSAFE.getObjectVolatile(ss, u)) == null) {
                    if (UNSAFE.compareAndSwapObject(ss, u, null, seg = s)) {
                        break;
                    }
                }
            }
        }

        return seg;
    }

    // Hash-based segment and entry accesses

    /**
     * Get the segment for the given hash
     */
    @SuppressWarnings("unchecked")
    private Segment<K, V> segmentForHash(int h) {
        long u = (((h >>> segmentShift) & segmentMask) << SSHIFT) + SBASE;
        return (Segment<K, V>) UNSAFE.getObjectVolatile(segments, u);
    }

    /**
     * 根据 hash 值，从指定的 segment 中获取数据槽（链首节点）
     */
    @SuppressWarnings("unchecked")
    static final <K, V> HashEntry<K, V> entryForHash(Segment<K, V> seg, int h) {
        HashEntry<K, V>[] tab;

        return (seg == null || (tab = seg.table) == null) ? null :
                (HashEntry<K, V>) UNSAFE.getObjectVolatile
                        (tab, ((long) (((tab.length - 1) & h)) << TSHIFT) + TBASE);
    }

    /* ---------------- Public operations -------------- */

    /**
     * Creates a new, empty map with the specified initial
     * capacity, load factor and concurrency level.
     * <p>
     * 为了节省内存空间，采用惰性构造的方式创建 segment ，在初始构造时，仅创建一个 segment
     * <p>
     * segment 表的容量最小为 2 的幕，以避免在惰性构造后，进行 put 操作时，再次调整数据槽容量
     *
     * @param initialCapacity  the initial capacity. The implementation
     *                         performs internal sizing to accommodate this many elements.
     * @param loadFactor       the load factor threshold, used to control resizing.
     *                         Resizing may be performed when the average number of elements per
     *                         bin exceeds this threshold.
     * @param concurrencyLevel the estimated number of concurrently
     *                         updating threads. The implementation performs internal sizing
     *                         to try to accommodate this many threads.
     * @throws IllegalArgumentException if the initial capacity is
     *                                  negative or the load factor or concurrencyLevel are
     *                                  nonpositive.
     */
    @SuppressWarnings("unchecked")
    public ConcurrentHashMap(int initialCapacity,
                             float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();

        if (concurrencyLevel > MAX_SEGMENTS)
            concurrencyLevel = MAX_SEGMENTS;

        // Find power-of-two sizes best matching arguments
        int sshift = 0;
        int ssize = 1; // segments 数据槽数组的长度

        while (ssize < concurrencyLevel) {
            ++sshift;
            ssize <<= 1;
        }

        this.segmentShift = 32 - sshift;
        this.segmentMask = ssize - 1;

        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        int c = initialCapacity / ssize;

        if (c * ssize < initialCapacity)
            ++c;

        // segment 分段锁（hash 表）节点的数据槽的数量
        int cap = MIN_SEGMENT_TABLE_CAPACITY;

        while (cap < c)
            cap <<= 1;

        // 为了节省内存空间，采用惰性构造的方式创建 segment ，在初始构造时，仅创建一个 segment
        Segment<K, V> s0 =
                new Segment<>(loadFactor, (int) (cap * loadFactor),
                        (HashEntry<K, V>[]) new HashEntry[cap]);

        Segment<K, V>[] ss = (Segment<K, V>[]) new Segment[ssize];

        UNSAFE.putOrderedObject(ss, SBASE, s0); // ordered write of segments[0]

        this.segments = ss;
    }

    /**
     * Creates a new, empty map with the specified initial capacity
     * and load factor and with the default concurrencyLevel (16).
     *
     * @param initialCapacity The implementation performs internal
     *                        sizing to accommodate this many elements.
     * @param loadFactor      the load factor threshold, used to control resizing.
     *                        Resizing may be performed when the average number of elements per
     *                        bin exceeds this threshold.
     * @throws IllegalArgumentException if the initial capacity of
     *                                  elements is negative or the load factor is nonpositive
     * @since 1.6
     */
    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL);
    }

    /**
     * Creates a new, empty map with the specified initial capacity,
     * and with default load factor (0.75) and concurrencyLevel (16).
     *
     * @param initialCapacity the initial capacity. The implementation
     *                        performs internal sizing to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of
     *                                  elements is negative.
     */
    public ConcurrentHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }

    /**
     * Creates a new, empty map with a default initial capacity (16),
     * load factor (0.75) and concurrencyLevel (16).
     */
    public ConcurrentHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }

    /**
     * Creates a new map with the same mappings as the given map.
     * The map is created with a capacity of 1.5 times the number
     * of mappings in the given map or 16 (whichever is greater),
     * and a default load factor (0.75) and concurrencyLevel (16).
     *
     * @param m the map
     */
    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                DEFAULT_INITIAL_CAPACITY),
                DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
        putAll(m);
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        /*
         * Sum per-segment modCounts to avoid mis-reporting when
         * elements are concurrently added and removed in one segment
         * while checking another, in which case the table was never
         * actually empty at any point. (The sum ensures accuracy up
         * through at least 1<<31 per-segment modifications before
         * recheck.)  Methods size() and containsValue() use similar
         * constructions for stability checks.
         */
        long sum = 0L;
        final Segment<K, V>[] segments = this.segments;
        for (int j = 0; j < segments.length; ++j) {
            Segment<K, V> seg = segmentAt(segments, j);
            if (seg != null) {
                if (seg.count != 0)
                    return false;
                sum += seg.modCount;
            }
        }
        if (sum != 0L) {
            // recheck unless no modifications
            for (int j = 0; j < segments.length; ++j) {
                Segment<K, V> seg = segmentAt(segments, j);
                if (seg != null) {
                    if (seg.count != 0)
                        return false;
                    sum -= seg.modCount;
                }
            }
            if (sum != 0L)
                return false;
        }
        return true;
    }

    /**
     * Returns the number of key-value mappings in this map.  If the
     * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        // Try a few times to get accurate count. On failure due to
        // continuous async changes in table, resort to locking.
        final Segment<K, V>[] segments = this.segments;
        int size;
        boolean overflow; // true if size overflows 32 bits
        long sum;         // sum of modCounts
        long last = 0L;   // previous sum
        int retries = -1; // first iteration isn't retry
        try {
            for (; ; ) {
                if (retries++ == RETRIES_BEFORE_LOCK) {
                    for (int j = 0; j < segments.length; ++j)
                        ensureSegment(j).lock(); // force creation
                }
                sum = 0L;
                size = 0;
                overflow = false;
                for (int j = 0; j < segments.length; ++j) {
                    Segment<K, V> seg = segmentAt(segments, j);
                    if (seg != null) {
                        sum += seg.modCount;
                        int c = seg.count;
                        if (c < 0 || (size += c) < 0)
                            overflow = true;
                    }
                }
                if (sum == last)
                    break;
                last = sum;
            }
        } finally {
            if (retries > RETRIES_BEFORE_LOCK) {
                for (int j = 0; j < segments.length; ++j)
                    segmentAt(segments, j).unlock();
            }
        }
        return overflow ? Integer.MAX_VALUE : size;
    }

    /**
     * 根据 key 获取 value
     *
     * @throws NullPointerException if the specified key is null
     */
    public V get(Object key) {
        Segment<K, V> s; // manually integrate access methods to reduce overhead
        HashEntry<K, V>[] tab;

        int h = hash(key.hashCode());
        long u = (((h >>> segmentShift) & segmentMask) << SSHIFT) + SBASE;

        /*
         * 根据 hash 值，从 segments 数组中获取 segment，
         * 如果目标位置的 segment 不为 null 并且 segment 的 hash 表不会 null，
         *
         * 防止出现 在 get 操作时，指定位置的 segment 还尚未构建
         * */
        if ((s = (Segment<K, V>) UNSAFE.getObjectVolatile(segments, u)) != null &&
                (tab = s.table) != null) {

            //#region 获取 hash 表中目标索引位置的数据槽的链首节点，遍历该数据槽，查找与 key 相同的节点，如果找到则直接返回该节点的 value
            for (
                    HashEntry<K, V> e = (HashEntry<K, V>) UNSAFE.getObjectVolatile(tab, ((long) (((tab.length - 1) & h)) << TSHIFT) + TBASE);
                    e != null;
                    e = e.next
            ) {
                K k;
                if ((k = e.key) == key || (e.hash == h && key.equals(k)))
                    return e.value;
            }
            //#endregion
        }
        return null;
    }

    /**
     * 判断指定的 key 是否存在于此表中
     * <p>
     * 与 get 方法相同，但不需要读取值
     *
     * @param key possible key
     * @return <tt>true</tt> if and only if the specified object
     * is a key in this table, as determined by the
     * <tt>equals</tt> method; <tt>false</tt> otherwise.
     * @throws NullPointerException if the specified key is null
     */
    @SuppressWarnings("unchecked")
    public boolean containsKey(Object key) {
        Segment<K, V> s;
        HashEntry<K, V>[] tab;
        int h = hash(key.hashCode());
        long u = (((h >>> segmentShift) & segmentMask) << SSHIFT) + SBASE;
        if ((s = (Segment<K, V>) UNSAFE.getObjectVolatile(segments, u)) != null &&
                (tab = s.table) != null) {
            for (
                    HashEntry<K, V> e = (HashEntry<K, V>) UNSAFE.getObjectVolatile(tab, ((long) (((tab.length - 1) & h)) << TSHIFT) + TBASE);
                    e != null;
                    e = e.next
            ) {
                K k;
                if ((k = e.key) == key || (e.hash == h && key.equals(k)))
                    return true;
            }
        }
        return false;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value. Note: This method requires a full internal
     * traversal of the hash table, and so is much slower than
     * method <tt>containsKey</tt>.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     * specified value
     * @throws NullPointerException if the specified value is null
     */
    public boolean containsValue(Object value) {
        // Same idea as size()
        if (value == null)
            throw new NullPointerException();
        final Segment<K, V>[] segments = this.segments;
        boolean found = false;
        long last = 0;
        int retries = -1;
        try {
            outer:
            for (; ; ) {
                if (retries++ == RETRIES_BEFORE_LOCK) {
                    for (int j = 0; j < segments.length; ++j)
                        ensureSegment(j).lock(); // force creation
                }
                long hashSum = 0L;
                int sum = 0;
                for (int j = 0; j < segments.length; ++j) {
                    HashEntry<K, V>[] tab;
                    Segment<K, V> seg = segmentAt(segments, j);
                    if (seg != null && (tab = seg.table) != null) {
                        for (int i = 0; i < tab.length; i++) {
                            HashEntry<K, V> e;
                            for (e = entryAt(tab, i); e != null; e = e.next) {
                                V v = e.value;
                                if (v != null && value.equals(v)) {
                                    found = true;
                                    break outer;
                                }
                            }
                        }
                        sum += seg.modCount;
                    }
                }
                if (retries > 0 && sum == last)
                    break;
                last = sum;
            }
        } finally {
            if (retries > RETRIES_BEFORE_LOCK) {
                for (int j = 0; j < segments.length; ++j)
                    segmentAt(segments, j).unlock();
            }
        }
        return found;
    }

    /**
     * Legacy method testing if some key maps into the specified value
     * in this table.  This method is identical in functionality to
     * {@link #containsValue}, and exists solely to ensure
     * full compatibility with class {@link java.util.Hashtable},
     * which supported this method prior to introduction of the
     * Java Collections framework.
     *
     * @param value a value to search for
     * @return <tt>true</tt> if and only if some key maps to the
     * <tt>value</tt> argument in this table as
     * determined by the <tt>equals</tt> method;
     * <tt>false</tt> otherwise
     * @throws NullPointerException if the specified value is null
     */
    public boolean contains(Object value) {
        return containsValue(value);
    }

    /**
     * 通过索引位置获取对应的 segment ，并将 put 操作代理给 segment
     * （由于采用惰性构造，所以需在 ensureSegment 中，确保该索引上 segment 对象的存在 ）
     *
     * @throws NullPointerException if the specified key or value is null
     */
    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        Segment<K, V> s;

        if (value == null)
            throw new NullPointerException();

        int hash = hash(key.hashCode());
        int j = (hash >>> segmentShift) & segmentMask;

        // 如果目标索引位置上的 segment 尚未构造，确保 segment 对象的创建
        if ((s = (Segment<K, V>) UNSAFE.getObject(segments, (j << SSHIFT) + SBASE)) == null) //  in ensureSegment
            s = ensureSegment(j);

        return s.put(key, hash, value, false);
    }

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     * or <tt>null</tt> if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    @SuppressWarnings("unchecked")
    public V putIfAbsent(K key, V value) {
        Segment<K, V> s;

        if (value == null)
            throw new NullPointerException();

        int hash = hash(key.hashCode());
        int j = (hash >>> segmentShift) & segmentMask;

        if ((s = (Segment<K, V>) UNSAFE.getObject
                (segments, (j << SSHIFT) + SBASE)) == null)
            s = ensureSegment(j);

        return s.put(key, hash, value, true);
    }

    /**
     * Copies all of the mappings from the specified map to this one.
     * These mappings replace any mappings that this map had for any of the
     * keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    /**
     * Removes the key (and its corresponding value) from this map.
     * This method does nothing if the key is not in the map.
     *
     * @param key the key that needs to be removed
     * @return the previous value associated with <tt>key</tt>, or
     * <tt>null</tt> if there was no mapping for <tt>key</tt>
     * @throws NullPointerException if the specified key is null
     */
    public V remove(Object key) {
        int hash = hash(key.hashCode());
        Segment<K, V> s = segmentForHash(hash);
        return s == null ? null : s.remove(key, hash, null);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the specified key is null
     */
    public boolean remove(Object key, Object value) {
        int hash = hash(key.hashCode());
        Segment<K, V> s;
        return value != null && (s = segmentForHash(hash)) != null &&
                s.remove(key, hash, value) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if any of the arguments are null
     */
    public boolean replace(K key, V oldValue, V newValue) {
        int hash = hash(key.hashCode());
        if (oldValue == null || newValue == null)
            throw new NullPointerException();
        Segment<K, V> s = segmentForHash(hash);
        return s != null && s.replace(key, hash, oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     * or <tt>null</tt> if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    public V replace(K key, V value) {
        int hash = hash(key.hashCode());
        if (value == null)
            throw new NullPointerException();
        Segment<K, V> s = segmentForHash(hash);
        return s == null ? null : s.replace(key, hash, value);
    }

    /**
     * 遍历 segments 数组，将 clear 操作代理给所有 segment
     * <p>
     * 将 clear 代理给 segment 的好处在于：
     * 采用分而治之的思想，在 clear 操作过程中，一旦 segment 的 clear 操作完毕，
     * 就可以立即对外提供当前 segment 的更新操作
     */
    public void clear() {
        final Segment<K, V>[] segments = this.segments;

        for (int j = 0; j < segments.length; ++j) {
            Segment<K, V> s = segmentAt(segments, j);

            if (s != null)
                s.clear();
        }
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  The set supports element
     * removal, which removes the corresponding mapping from this map,
     * via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * <p>The view's <tt>iterator</tt> is a "weakly consistent" iterator
     * that will never throw {@link ConcurrentModificationException},
     * and guarantees to traverse elements as they existed upon
     * construction of the iterator, and may (but is not guaranteed to)
     * reflect any modifications subsequent to construction.
     */
    public Set<K> keySet() {
        Set<K> ks = keySet;
        return (ks != null) ? ks : (keySet = new KeySet());
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  The collection
     * supports element removal, which removes the corresponding
     * mapping from this map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt>, and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * <p>The view's <tt>iterator</tt> is a "weakly consistent" iterator
     * that will never throw {@link ConcurrentModificationException},
     * and guarantees to traverse elements as they existed upon
     * construction of the iterator, and may (but is not guaranteed to)
     * reflect any modifications subsequent to construction.
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        return (vs != null) ? vs : (values = new Values());
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  The set supports element
     * removal, which removes the corresponding mapping from the map,
     * via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * <p>The view's <tt>iterator</tt> is a "weakly consistent" iterator
     * that will never throw {@link ConcurrentModificationException},
     * and guarantees to traverse elements as they existed upon
     * construction of the iterator, and may (but is not guaranteed to)
     * reflect any modifications subsequent to construction.
     */
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es = entrySet;
        return (es != null) ? es : (entrySet = new EntrySet());
    }

    /**
     * Returns an enumeration of the keys in this table.
     *
     * @return an enumeration of the keys in this table
     * @see #keySet()
     */
    public Enumeration<K> keys() {
        return new KeyIterator();
    }

    /**
     * Returns an enumeration of the values in this table.
     *
     * @return an enumeration of the values in this table
     * @see #values()
     */
    public Enumeration<V> elements() {
        return new ValueIterator();
    }

    /* ---------------- Iterator Support -------------- */

    abstract class HashIterator {
        int nextSegmentIndex;
        int nextTableIndex;
        HashEntry<K, V>[] currentTable;
        HashEntry<K, V> nextEntry;
        HashEntry<K, V> lastReturned;

        HashIterator() {
            nextSegmentIndex = segments.length - 1;
            nextTableIndex = -1;
            advance();
        }

        /**
         * Set nextEntry to first node of next non-empty table
         * (in backwards order, to simplify checks).
         */
        final void advance() {
            for (; ; ) {
                if (nextTableIndex >= 0) {
                    if ((nextEntry = entryAt(currentTable,
                            nextTableIndex--)) != null)
                        break;
                } else if (nextSegmentIndex >= 0) {
                    Segment<K, V> seg = segmentAt(segments, nextSegmentIndex--);
                    if (seg != null && (currentTable = seg.table) != null)
                        nextTableIndex = currentTable.length - 1;
                } else
                    break;
            }
        }

        final HashEntry<K, V> nextEntry() {
            HashEntry<K, V> e = nextEntry;
            if (e == null)
                throw new NoSuchElementException();
            lastReturned = e; // cannot assign until after null check
            if ((nextEntry = e.next) == null)
                advance();
            return e;
        }

        public final boolean hasNext() {
            return nextEntry != null;
        }

        public final boolean hasMoreElements() {
            return nextEntry != null;
        }

        public final void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            ConcurrentHashMap.this.remove(lastReturned.key);
            lastReturned = null;
        }
    }

    final class KeyIterator
            extends HashIterator
            implements Iterator<K>, Enumeration<K> {
        public final K next() {
            return super.nextEntry().key;
        }

        public final K nextElement() {
            return super.nextEntry().key;
        }
    }

    final class ValueIterator
            extends HashIterator
            implements Iterator<V>, Enumeration<V> {
        public final V next() {
            return super.nextEntry().value;
        }

        public final V nextElement() {
            return super.nextEntry().value;
        }
    }

    /**
     * Custom Entry class used by EntryIterator.next(), that relays
     * setValue changes to the underlying map.
     */
    final class WriteThroughEntry
            extends AbstractMap.SimpleEntry<K, V> {
        WriteThroughEntry(K k, V v) {
            super(k, v);
        }

        /**
         * Set our entry's value and write through to the map. The
         * value to return is somewhat arbitrary here. Since a
         * WriteThroughEntry does not necessarily track asynchronous
         * changes, the most recent "previous" value could be
         * different from what we return (or could even have been
         * removed in which case the put will re-establish). We do not
         * and cannot guarantee more.
         */
        public V setValue(V value) {
            if (value == null) throw new NullPointerException();
            V v = super.setValue(value);
            ConcurrentHashMap.this.put(getKey(), value);
            return v;
        }
    }

    final class EntryIterator
            extends HashIterator
            implements Iterator<Entry<K, V>> {
        public Map.Entry<K, V> next() {
            HashEntry<K, V> e = super.nextEntry();
            return new WriteThroughEntry(e.key, e.value);
        }
    }

    final class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        public int size() {
            return ConcurrentHashMap.this.size();
        }

        public boolean isEmpty() {
            return ConcurrentHashMap.this.isEmpty();
        }

        public boolean contains(Object o) {
            return ConcurrentHashMap.this.containsKey(o);
        }

        public boolean remove(Object o) {
            return ConcurrentHashMap.this.remove(o) != null;
        }

        public void clear() {
            ConcurrentHashMap.this.clear();
        }
    }

    final class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        public int size() {
            return ConcurrentHashMap.this.size();
        }

        public boolean isEmpty() {
            return ConcurrentHashMap.this.isEmpty();
        }

        public boolean contains(Object o) {
            return ConcurrentHashMap.this.containsValue(o);
        }

        public void clear() {
            ConcurrentHashMap.this.clear();
        }
    }

    final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            V v = ConcurrentHashMap.this.get(e.getKey());
            return v != null && v.equals(e.getValue());
        }

        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return ConcurrentHashMap.this.remove(e.getKey(), e.getValue());
        }

        public int size() {
            return ConcurrentHashMap.this.size();
        }

        public boolean isEmpty() {
            return ConcurrentHashMap.this.isEmpty();
        }

        public void clear() {
            ConcurrentHashMap.this.clear();
        }
    }

    /* ---------------- Serialization Support -------------- */

    /**
     * Save the state of the <tt>ConcurrentHashMap</tt> instance to a
     * stream (i.e., serialize it).
     *
     * @param s the stream
     * @serialData the key (Object) and value (Object)
     * for each key-value mapping, followed by a null pair.
     * The key-value mappings are emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s) throws IOException {
        // force all segments for serialization compatibility
        for (int k = 0; k < segments.length; ++k)
            ensureSegment(k);
        s.defaultWriteObject();

        final Segment<K, V>[] segments = this.segments;
        for (int k = 0; k < segments.length; ++k) {
            Segment<K, V> seg = segmentAt(segments, k);
            seg.lock();
            try {
                HashEntry<K, V>[] tab = seg.table;
                for (int i = 0; i < tab.length; ++i) {
                    HashEntry<K, V> e;
                    for (e = entryAt(tab, i); e != null; e = e.next) {
                        s.writeObject(e.key);
                        s.writeObject(e.value);
                    }
                }
            } finally {
                seg.unlock();
            }
        }
        s.writeObject(null);
        s.writeObject(null);
    }

    /**
     * Reconstitute the <tt>ConcurrentHashMap</tt> instance from a
     * stream (i.e., deserialize it).
     *
     * @param s the stream
     */
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        // Re-initialize segments to be minimally sized, and let grow.
        int cap = MIN_SEGMENT_TABLE_CAPACITY;
        final Segment<K, V>[] segments = this.segments;
        for (int k = 0; k < segments.length; ++k) {
            Segment<K, V> seg = segments[k];
            if (seg != null) {
                seg.threshold = (int) (cap * seg.loadFactor);
                seg.table = (HashEntry<K, V>[]) new HashEntry[cap];
            }
        }

        // Read the keys and values, and put the mappings in the table
        for (; ; ) {
            K key = (K) s.readObject();
            V value = (V) s.readObject();
            if (key == null)
                break;
            put(key, value);
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long SBASE;
    private static final int SSHIFT;
    private static final long TBASE;
    private static final int TSHIFT;

    static {
        int ss, ts;
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class tc = HashEntry[].class;
            Class sc = Segment[].class;
            TBASE = UNSAFE.arrayBaseOffset(tc);
            SBASE = UNSAFE.arrayBaseOffset(sc);
            ts = UNSAFE.arrayIndexScale(tc);
            ss = UNSAFE.arrayIndexScale(sc);
        } catch (Exception e) {
            throw new Error(e);
        }
        if ((ss & (ss - 1)) != 0 || (ts & (ts - 1)) != 0)
            throw new Error("data type scale not a power of two");
        SSHIFT = 31 - Integer.numberOfLeadingZeros(ss);
        TSHIFT = 31 - Integer.numberOfLeadingZeros(ts);
    }

}
