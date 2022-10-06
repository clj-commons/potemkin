(ns potemkin.utils
  (:require
    [potemkin.macros :refer [unify-gensyms]]
    [clj-tuple :as t])
  (:import
    [java.util.concurrent
     ConcurrentHashMap]))

(defmacro ^{:deprecated true
            :no-doc true
            :superseded-by "clojure.core/bound-fn"} fast-bound-fn
  "Quite probably not faster than core bound-fn these days.

   ~45% slower in personal testing. Be sure to profile your use case."
  [& fn-body]
  (let [{:keys [major minor]} *clojure-version*
        use-thread-bindings? (and (= 1 major) (< minor 3))
        use-get-binding? (and (= 1 major) (< minor 4))]
    (if use-thread-bindings?
      `(let [bindings# (get-thread-bindings)
             f# (fn ~@fn-body)]
         (fn [~'& args#]
           (with-bindings bindings#
             (apply f# args#))))
      `(let [bound-frame# ~(if use-get-binding?
                             `(clojure.lang.Var/getThreadBindingFrame)
                             `(clojure.lang.Var/cloneThreadBindingFrame))
             f# (fn ~@fn-body)]
         (fn [~'& args#]
           (let [curr-frame# (clojure.lang.Var/getThreadBindingFrame)]
             (clojure.lang.Var/resetThreadBindingFrame bound-frame#)
             (try
               (apply f# args#)
               (finally
                 (clojure.lang.Var/resetThreadBindingFrame curr-frame#)))))))))

(defn ^{:deprecated true
        :no-doc true
        :superseded-by "clojure.core/bound-fn*"} fast-bound-fn*
  "Quite probably not faster than core bound-fn* these days.

   ~45% slower in personal testing. Be sure to profile your use case."
  [f]
  (fast-bound-fn [& args]
    (apply f args)))

(defn ^:no-doc retry-exception? [x]
  (= "clojure.lang.LockingTransaction$RetryEx" (.getName ^Class (class x))))

(defmacro ^:deprecated ^:no-doc try*
  "A variant of try that is fully transparent to transaction retry exceptions"
  [& body+catch]
  (let [body (take-while
               #(or (not (sequential? %)) (not (= 'catch (first %))))
               body+catch)
        catch (drop (count body) body+catch)
        ignore-retry (fn [x]
                       (when x
                         (let [ex (nth x 2)]
                           `(~@(take 3 x)
                             (if (potemkin.utils/retry-exception? ~ex)
                               (throw ~ex)
                               (do ~@(drop 3 x)))))))
        class->clause (-> (zipmap (map second catch) catch)
                        (update-in ['Throwable] ignore-retry)
                        (update-in ['Error] ignore-retry))]
    `(try
       ~@body
       ~@(->> class->clause vals (remove nil?)))))

(defmacro condp-case
  "A variant of condp which has case-like syntax for options.  When comparing
   smaller numbers of keywords, this can be faster, sometimes significantly."
  [predicate value & cases]
  (unify-gensyms
    `(let [val## ~value
           pred## ~predicate]
       (cond
         ~@(->> cases
             (partition 2)
             (map
               (fn [[vals expr]]
                 `(~(if (sequential? vals)
                      `(or ~@(map (fn [x] `(pred## val## ~x)) vals))
                      `(pred## val## ~vals))
                   ~expr)))
             (apply concat))
         :else
         ~(if (even? (count cases))
            `(throw (IllegalArgumentException. (str "no matching clause for " (pr-str val##))))
            (last cases))))))

;;; fast-memoize

(definline ^:no-doc re-nil [x]
  `(let [x# ~x]
     (if (identical? ::nil x#) nil x#)))

(definline ^:no-doc de-nil [x]
  `(let [x# ~x]
     (if (nil? x#) ::nil x#)))

(defmacro ^:no-doc memoize-form [m f & args]
  `(let [k# (t/vector ~@args)]
     (let [v# (.get ~m k#)]
       (if-not (nil? v#)
         (re-nil v#)
         (let [v# (de-nil (~f ~@args))]
           (re-nil (or (.putIfAbsent ~m k# v#) v#)))))))

(defn ^{:deprecated true
        :no-doc true
        :superseded-by "clojure.core/memoize"} fast-memoize
  "Quite possibly not faster than core memoize any more.
   See https://github.com/clj-commons/byte-streams/pull/50 and profile your use case."
  [f]
  (let [m (ConcurrentHashMap.)]
    (fn
      ([]
         (memoize-form m f))
      ([x]
         (memoize-form m f x))
      ([x y]
         (memoize-form m f x y))
      ([x y z]
         (memoize-form m f x y z))
      ([x y z w]
         (memoize-form m f x y z w))
      ([x y z w u]
         (memoize-form m f x y z w u))
      ([x y z w u v]
         (memoize-form m f x y z w u v))
      ([x y z w u v & rest]
         (let [k (list* x y z w u v rest)]
           (let [v (.get ^ConcurrentHashMap m k)]
             (if-not (nil? v)
               (re-nil v)
               (let [v (de-nil (apply f k))]
                 (or (.putIfAbsent m k v) v)))))))))

;;;

(defmacro doit
  "An iterable-based version of doseq that doesn't emit inline-destroying chunked-seq code."
  [[x it] & body]
  (let [it-sym (gensym "iterable")]
    `(let [~it-sym ~it
           it# (.iterator ~(with-meta it-sym {:tag "Iterable"}))]
       (loop []
         (when (.hasNext it#)
           (let [~x (.next it#)]
            ~@body)
           (recur))))))

(defmacro doary
  "An array-specific version of doseq."
  [[x ary] & body]
  (let [ary-sym (gensym "ary")]
    `(let [~(with-meta ary-sym {:tag "objects"}) ~ary]
       (dotimes [idx# (alength ~ary-sym)]
         (let [~x (aget ~ary-sym idx#)]
           ~@body)))))



(comment
  (defn foo [nums]
    (reduce + nums))

  (def memo-foo (memoize foo))
  (def fast-memo-foo (fast-memoize foo))

  (def quads (vec (take 100 (cycle (partition 4 (range 100))))))

  (bench (run! memo-foo quads))

  (bench (run! fast-memo-foo quads))


  (defn exercise-vec
    [v]
    (let [c (count v)
          oneth (nth v 0 -20)]
      (reduce + (+ c oneth) v)))


  ;; monomorphic vs megamorphic comparison
  (let [v0 (vector)
        v1 (vector 1)
        v2 (vector 1 2)
        v3 (vector 1 2 3 4 5)]
    (bench
      (do
        (exercise-vec v0)
        (exercise-vec v1)
        (exercise-vec v2)
        (exercise-vec v3))))
  (let [v0 (t/vector)
        v1 (t/vector 1)
        v2 (t/vector 1 2)
        v3 (t/vector 1 2 3 4 5)]
    (bench
      (do
        (exercise-vec v0)
        (exercise-vec v1)
        (exercise-vec v2)
        (exercise-vec v3))))

  (bench
    (do
      (vector)
      (vector 1)
      (vector 1 :two)
      (vector 1 2 :three 4 :five)
      (vector 1 2 3 4 5 :6 "7" :8 'symbol9 :10)))
  (bench
    (do
      (t/vector)
      (t/vector 1)
      (t/vector 1 :two)
      (t/vector 1 2 :three 4 :five)
      (t/vector 1 2 3 4 5 :6 "7" :8 'symbol9 :10)))

  (bench {1 2 3 4 5 :asd "123" 6 7 8})
  (bench (array-map 1 2 3 4 5 :asd "123" 6 7 8))
  (bench (hash-map 1 2 3 4 5 :asd "123" 6 7 8))
  (bench (t/hash-map 1 2 3 4 5 :asd "123" 6 7 8))

  (defn fast-vector
    ([] [])
    ([a] [a])
    ([a b] [a b])
    ([a b c] [a b c])
    ([a b c d] [a b c d])
    ([a b c d e] [a b c d e])
    ([a b c d e f] [a b c d e f])
    ([a b c d e f & args]
     (. clojure.lang.LazilyPersistentVector (create (into [a b c d e f] args)))))
  (bench (fast-vector 1 2 3 4 5 :asd "123" 6 7 8))
  )
