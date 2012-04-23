;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin.map
  (:use
    [clojure.walk]
    [potemkin protocols])
  (:import
    java.util.Map$Entry
    clojure.lang.MapEntry
    clojure.lang.IPersistentVector))

(defn override-deftype-template [template overrides]
  (let [signature #(when (sequential? %) (take 2 %))
        signatures (->> template signature (remove nil?) set)]
    (concat
      (remove #(and (sequential? %) (signatures %)) template)
      template)))

(defn strip-namespaces [s]
  (if (and (symbol? s) (= "potemkin.map" (namespace s)))
    (-> s name symbol)
    s))

(defn map-template
  ([name &
    {unwrapper :unwrapper
     wrapper :wrapper
     args :args
     get-generator :get
     assoc-generator :assoc
     dissoc-generator :dissoc
     keys-generator :keys}]
     (let [unwrapped-data (if unwrapper
                            `(~unwrapper ~'data)
                            'data)
           wrapped-data (if wrapper
                          (fn [x] `(~wrapper ~x))
                          identity)
           throw-arity (fn [actual]
                         `(throw
                            (RuntimeException.
                              ~(str "Wrong number of args (" actual ") passed to: " name))))]
       (postwalk strip-namespaces
         `(deftype ~name [data ~@args]
            clojure.lang.MapEquivalence

            potemkin.protocols.PotemkinMap
            (keys* [this data]
              ~(if-not keys-generator
                 `(keys ~'data)
                 `~((eval keys-generator) name 'data 'this)))

            clojure.lang.IPersistentCollection
            (equiv [this x]
              (and
               (map? x)
               (= x (into {} this))))
            (cons [this o]
              (if-let [[k v] (seq o)]
                (assoc this k v)
                this))

            clojure.lang.Counted
            (count [this]
              (count (potemkin.protocols/keys* this ~unwrapped-data)))

            clojure.lang.Seqable
            (seq [this]
              (map #(MapEntry. % (.valAt this % nil)) (potemkin.protocols/keys* this ~unwrapped-data)))

            ~@(when (or (> (:major *clojure-version*) 1)
                       (and (= (:major *clojure-version*) 1)
                            (>= (:minor *clojure-version*) 4)))
               `(clojure.core.protocols.CollReduce
                 (coll-reduce
                   [this f]
                   (reduce f (seq this)))
                 (coll-reduce
                   [this f val#]
                   (reduce f val# (seq this)))))
            
            ;; clojure.core.protocols.CollReduce
            ;; (coll-reduce
            ;;   [this f]
            ;;   (reduce f (seq this)))

            ;; (coll-reduce
            ;;   [this f val#]
            ;;   (reduce f val# (seq this)))
            
            Object
            (hashCode [this]
              (reduce
               (fn [acc [k v]]
                 (unchecked-add acc (bit-xor (hash k) (hash v))))
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
              ~(if get-generator
                 ((eval get-generator) name 'data 'this 'k 'default)
                 `(get ~unwrapped-data k default)))

            clojure.lang.Associative
            (containsKey [this k]
              (contains? (.keySet this) k))
            (entryAt [this k]
              (let [v (.valAt this k nil)]
                (reify java.util.Map$Entry
                  (getKey [_] k)
                  (getValue [_] v))))
            (assoc [this k v]
              ~(if assoc-generator
                 ((eval assoc-generator) name 'data 'this 'k 'v)
                 `(new ~name ~(wrapped-data `(assoc ~unwrapped-data k v)) ~@args)))

            java.util.Map
            (get [this k]
              (.valAt this k))
            (isEmpty [this]
              (empty? this))
            (size [this]
              (count this))
            (keySet [this]
              (set (potemkin.protocols/keys* this ~unwrapped-data)))
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
            clojure.lang.IPersistentMap
            (assocEx [this k v]
              (if (contains? ~unwrapped-data k)
                (throw (Exception. "Key or value already present"))
                (assoc this k v)))
            (without [this k]
              ~(if dissoc-generator
                 ((eval dissoc-generator) name 'data 'this 'k)
                 `(new ~name ~(wrapped-data `(dissoc ~unwrapped-data k)) ~@args)))

            java.util.concurrent.Callable
            (call [this]
              ~(throw-arity 0))

            java.lang.Runnable
            (run [this]
              ~(throw-arity 0))

            clojure.lang.IFn
            (invoke [this]
              ~(throw-arity 0))
            (invoke [this k]
              (.valAt this k))
            (invoke [this k not-found]
              (.valAt this k not-found))
            ~@(map
                (fn [n]
                  `(invoke [this ~@(repeat n '_)]
                           ~(throw-arity n)))
                (range 3 21))

            (applyTo [this args]
              (let [cnt (count args)]
                (case cnt
                  1 (this (first args))
                  2 (this (first args) (second args))
                  ~(throw-arity 'cnt)))))
         ))))

(defmacro def-custom-map
  "Allows the creation of a custom map data structure that sits atop a normal Clojure hash-map,
   and behaves in all ways like a normal hash-map.

   By default, the type created has a single field: 'data', which contains either a hash-map or
   something that can be turned into a hash-map.  If 'data', for instance, is an atom containing
   a map then you can define data wrappers and unwrappers like so:

   (def-custom-map Foo :unwrapper deref, :wrapper atom)

   It's also possible to define new definitions for get, keys, assoc, and dissoc operations.
   These overrides must be functions that accept the following arguments, respectively:

   :get    (fn [type-name data this key default-value] ...)
   :assoc  (fn [type-name data this key value] ...)
   :dissoc (fn [type-name data this key] ...)
   :keys   (fn [type-name data this] ...)

   The function should then return quoted syntax that performs the specified operation as
   is appropriate.  A simple 'lazy' map that auto-dereferences values defined with (delay ...)
   would look like this:

   (def-custom-map LazyMap
     :get (fn [_ data _ key default-value]
            `(if-not (contains? ~data ~key)
              ~default-value
              (let [val# (get ~data ~key)]
                (if (delay? val#)
                  @val#
                  val#)))))

  "
  [name & {overrides :overrides :as args}]
  `(do
     ~(apply map-template name (apply concat args))))
