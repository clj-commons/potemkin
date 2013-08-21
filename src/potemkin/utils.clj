(ns potemkin.utils
  (:use
    [potemkin macros collections])
  (:import
    [java.util
     HashMap]))

(defmacro fast-bound-fn
  "Creates a variant of bound-fn which doesn't assume you want a merged
   context between the source and execution environments."
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

(defn fast-bound-fn*
  "Creates a function which conveys bindings, via fast-bound-fn."
  [f]
  (fast-bound-fn [& args]
    (apply f args)))

(defmacro try*
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
                             (if (lamina.core.utils/retry-exception? ~ex)
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
         ~(when-not (even? (count cases))
            `(throw (IllegalArgumentException. (str "no matching clause for " (pr-str val##))))
            (last cases))))))

;;; fast-memoize

(definline re-nil [x]
  `(let [x# ~x]
     (if (identical? ::nil x#) nil x#)))

(definline de-nil [x]
  `(let [x# ~x]
     (if (nil? x#) ::nil x#)))

(defn add! [^HashMap m k v]
  (doto (HashMap. m) (.put k v)))

(defmacro memoize-form [m f & args]
  `(let [k# (tuple ~@args)]
     (if-let [v# (.get ^HashMap (deref ~m) k#)]
       (re-nil v#)
       (let [v# (de-nil (~f ~@args))]
         (swap! ~m add! k# v#)
         v#))))

(defn fast-memoize
  "A version of `memoize` which is "
  [f]
  (let [m (atom (HashMap.))]
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
      ([x y z w & rest]
         (let [k (apply vector x y z w rest)]
           (if-let [v (.get ^HashMap (deref m) k)]
             (re-nil v)
             (let [v (de-nil (apply f k))]
               (swap! m add! k v)
               v)))))))
