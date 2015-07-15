(ns potemkin.collections
  (:use
    [potemkin types macros utils]))

(defprotocol PotemkinMap
  (empty* [m])
  (get* [m k default])
  (assoc* [m k v])
  (dissoc* [m k])
  (keys* [m])
  (with-meta* [o mta])
  (meta* [o]))

(defprotocol PotemkinMeta
  (meta-atom [_])
  (with-meta-atom [_ x]))

(defn throw-arity [actual]
  `(throw
     (RuntimeException.
       ~(str "Wrong number of args (" actual ")"))))

(defmacro compile-if [test then else]
  (if (eval test)
    then
    else))

(eval
  (unify-gensyms
    `(def-abstract-type PotemkinFn
       java.util.concurrent.Callable
       (call [this##]
         (.invoke ~(with-meta `this## {:tag "clojure.lang.IFn"})))

       java.lang.Runnable
       (run [this##]
         (.invoke ~(with-meta `this## {:tag "clojure.lang.IFn"})))

       clojure.lang.IFn
       ~@(map
           (fn [n]
             `(~'invoke [this# ~@(repeat n '_)]
                ~(throw-arity n)))
           (range 0 21))

       (applyTo [this## args##]
         (let [cnt# (count args##)]
           (case cnt#
             ~@(mapcat
                 (fn [n]
                   `[~n (.invoke
                          ~(with-meta `this## {:tag "clojure.lang.IFn"})
                          ~@(map (fn [arg] `(nth args## ~arg)) (range n)))])
                 (range 0 21))))))))

(def-abstract-type AbstractMap

  potemkin.collections.PotemkinMap

  clojure.lang.MapEquivalence

  clojure.lang.IPersistentCollection

  (equiv [this x]
    (and (map? x) (= x (into {} this))))

  (cons [this o]
    (if (map? o)
      (reduce #(apply assoc %1 %2) this o)
      (if-let [[k v] (seq o)]
        (assoc this k v)
        this)))

  clojure.lang.IObj
  (withMeta [this mta]
    (potemkin.collections/with-meta* this mta))
  (meta [this]
    (potemkin.collections/meta* this))
  (meta* [this]
    nil)

  clojure.lang.Counted

  (count [this]
    (count (potemkin.collections/keys* this)))

  clojure.lang.Seqable
  (seq [this]
    (seq
      (map
        #(potemkin.PersistentMapProxy$MapEntry. this %)
        (potemkin.collections/keys* this))))

  ^{:min-version "1.4.0"}
  clojure.core.protocols.CollReduce

  ^{:min-version "1.4.0"}
  (coll-reduce
    [this f]
    (reduce f (seq this)))

  ^{:min-version "1.4.0"}
  (coll-reduce
    [this f val#]
    (reduce f val# (seq this)))

  clojure.lang.IHashEq
  (hasheq [this]
    (potemkin.collections/compile-if (resolve 'clojure.core/hash-unordered-coll)
      (hash-unordered-coll (or (seq this) ()))
      (reduce
        (fn [acc [k v]]
          (unchecked-add acc (bit-xor (hash k) (hash v))))
        0
        (seq this))))

  Object
  (hashCode [this]
    (reduce
      (fn [acc [k v]]
        (unchecked-add acc (bit-xor (.hashCode k) (.hashCode v))))
      0
      (seq this)))

  (equals [this x]
    (or (identical? this x)
      (and
        (map? x)
        (= x (into {} this)))))

  (toString [this]
    (str (into {} this)))

  clojure.lang.ILookup
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k default]
    (potemkin.collections/get* this k default))

  clojure.lang.Associative
  (containsKey [this k]
    (contains? (.keySet this) k))

  (entryAt [this k]
    (when (contains? (.keySet this) k)
      (potemkin.PersistentMapProxy$MapEntry. this k)))

  (assoc [this k v]
    (potemkin.collections/assoc* this k v))

  (empty* [this]
    {})

  (empty [this]
    (potemkin.collections/empty* this))

  java.util.Map
  (get [this k]
    (.valAt this k))
  (isEmpty [this]
    (empty? this))
  (size [this]
    (count this))
  (keySet [this]
    (set (potemkin.collections/keys* this)))
  (put [_ _ _]
    (throw (UnsupportedOperationException.)))
  (putAll [_ _]
    (throw (UnsupportedOperationException.)))
  (clear [_]
    (throw (UnsupportedOperationException.)))
  (remove [_ _]
    (throw (UnsupportedOperationException.)))
  (values [this]
    (->> this seq (map second)))
  (entrySet [this]
    (->> this seq set))

  java.util.Iterator
  (iterator [this]
    (clojure.lang.SeqIterator. this))

  clojure.lang.IPersistentMap
  (assocEx [this k v]
    (if (contains? this k)
      (throw (Exception. "Key or value already present"))
      (assoc this k v)))
  (without [this k]
    (potemkin.collections/dissoc* this k))

  potemkin.collections/PotemkinFn

  (invoke [this k]
    (potemkin.collections/get* this k nil))
  (invoke [this k default]
    (potemkin.collections/get* this k default)))

(defmacro def-map-type
  "Like deftype, but must contain definitions for the following functions:

   (get [this key default-value])
   (assoc [this key value])
   (dissoc [this key])
   (keys [this])
   (meta [this])
   (with-meta [this meta])

   All other necessary functions will be defined so that this behaves like a normal
   Clojure map.  These can be overriden, if desired."
  [name params & body]
  (let [fns '{get get*
              assoc assoc*
              dissoc dissoc*
              keys keys*
              empty empty*
              with-meta with-meta*
              meta meta*}
        classname (with-meta (symbol (str (namespace-munge *ns*) "." name)) (meta name))]
    (unify-gensyms
      `(do
         (deftype+ ~name ~params ~'potemkin.collections/AbstractMap
           ~@(map
               #(if (sequential? %)
                  (list* (get fns (first %) (first %)) (rest %))
                  %)
               body))
         ~classname))))

(defmacro reify-map-type
  "Like reify, but must contain definitions for the following functions:

   (get [this key default-value])
   (assoc [this key value])
   (dissoc [this key])
   (keys [this])

   All other necessary functions will be defined so that this behaves like a normal
   Clojure map.  These can be overriden, if desired."
  [& body]
  (let [fns '{get get*
              assoc assoc*
              dissoc dissoc*
              keys keys*
              empty empty*}
        elide? '#{withMeta meta}]
    (->>
      `(reify+ ~'potemkin.collections/AbstractMap
         ~@(map
             #(if (sequential? %)
                (list* (get fns (first %) (first %)) (rest %))
                %)
             body))
      macroexpand
      (remove
        #(if (sequential? %)
           (elide? (first %))
           false)))))

(defmacro def-derived-map
  "Allows a map type to be defined where key-value pairs may be derived from fields.

   For instance, if we want to create a map which contains both upper and lower-case
   versions of a string without immediately instantiating both, we can do this:

   (def-derived-map StringMap [^String s]
     :lower-case (.toLowerCase s)
     :upper-case (.toUpperCase s))

   The resulting map will behave correctly if the defined keys are removed, shadowed,
   etc.

   The above class will automatically create a constructor named '->StringMap'."
  [name params & {:as m}]
  (let [interface (symbol (str "ILookup" name))
        methods (->> (count m) range (map #(symbol (str "get__" %))))
        key-set (set (keys m))]
    (unify-gensyms
      `(do

         (definterface ~interface
           ~@(map
               #(list % [])
               methods))

         (def-map-type ~name ~(vec (conj params `key-set## `added## `meta##))

           ~interface
           ~@(->> (map vector methods (vals m))
               (map
                 (fn [[name f]]
                   (list name `[_#] f))))

           (~'meta [_] meta##)

           (~'with-meta [_ x#]
             (new ~name ~@params key-set## added## x#))

           (~'get [this## key# default-value#]
             (if-let [e# (find added## key#)]
               (val e#)
               (if (contains? key-set## key#)
                 (case key#
                   ~@(interleave
                       (keys m)
                       (map (fn [m] `(~(symbol (str "." m)) this##)) methods))
                   default-value#)
                 default-value#)))

           (~'keys [this#]
             key-set##)

           (~'assoc [this# key# value#]
             (new ~name ~@params
               (conj key-set## key#)
               (assoc added## key# value#)
               meta##))

           (~'dissoc [this# key#]
             (new ~name ~@params
               (disj key-set## key#)
               (dissoc added## key#)
               meta##)))

         (let [key-set# ~key-set]
           (defn ~(symbol (str "->" name)) [~@params]
             (new ~name ~@params key-set# nil nil)))))))
