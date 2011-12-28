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
            clojure.lang.IPersistentCollection
            potemkin.protocols.PotemkinMap
            (keys* [this data]
              ~(if-not keys-generator
                 `(keys ~'data)
                 `~((eval keys-generator) name 'data 'this)))
            (equiv [this x]
              (and
                (map? x)
                (= x (into {} this))))
            (cons [this o]
              (cond
                (instance? Map$Entry o)
                (let [[a b] o]
                  (assoc this a b))
                (instance? IPersistentVector o)
                (do
                  (assert (= 2 (count o)))
                  (let [[a b] o]
                    (assoc this a b)))
                :else
                (reduce conj this o)))
            clojure.lang.Counted
            (count [this]
              (count (potemkin.protocols/keys* this ~unwrapped-data)))
            clojure.lang.Seqable
            (seq [this]
              (map #(MapEntry. % (.valAt this % nil)) (potemkin.protocols/keys* this ~unwrapped-data)))
            Object
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
            (invoke [this a1 a2 a3]
              ~(throw-arity 3))
            (invoke [this a1 a2 a3 a4]
              ~(throw-arity 4))
            (invoke [this a1 a2 a3 a4 a5]
              ~(throw-arity 5))
            (invoke [this a1 a2 a3 a4 a5 a6]
              ~(throw-arity 6))
            (invoke [this a1 a2 a3 a4 a5 a6 a7]
              ~(throw-arity 7))
            (invoke [this a1 a2 a3 a4 a5 a6 a7 a8]
              ~(throw-arity 8))
            (invoke [this a1 a2 a3 a4 a5 a6 a7 a8 a9]
              ~(throw-arity 9))
            (invoke [this a1 a2 a3 a4 a5 a6 a7 a8 a9 a10]
              ~(throw-arity 10))
            (invoke [this a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11]
              ~(throw-arity 11))
            (invoke [this a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12]
              ~(throw-arity 12))
            (invoke [this a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13]
              ~(throw-arity 13))
            (invoke [this a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14]
              ~(throw-arity 14))
            (invoke [this a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15]
              ~(throw-arity 15))
            (invoke [this a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16]
              ~(throw-arity 16))
            (invoke [this a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17]
              ~(throw-arity 17))
            (invoke [this a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18]
              ~(throw-arity 18))
            (invoke [this a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19]
              ~(throw-arity 19))
            (invoke [this a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20]
              ~(throw-arity 20))
            (applyTo [this args]
              (case (count args)
                1 (.invoke this (first args))
                2 (.invoke this (first args) (second args))
                ~(throw-arity (count args)))))
         ))))

(defmacro def-custom-map
  "Allows the creation of a custom map data structure that sits atop a normal Clojure hash-map,
   and behaves in all ways like a 

   By default, the type created has a single field: 'data', which contains either a hash-map or
   something that can be turned into a hash-map.  If 'data', for instance, is an atom containing
   a map then you can define data wrappers and unwrappers like so:

   (def-custom-map Foo :unwrapper deref, :wrapper atom)

   It's also possible to define new definitions for get, seq, assoc, and dissoc operations.
   These overrides must be functions that accept the following arguments, respectively:

   :get    (fn [type-name data this key default-value] ...)
   :assoc  (fn [type-name data this key value] ...)
   :dissoc (fn [type-name data this key] ...)
   :keys   (fn [type-name data this] ...)

   The function should then return quoted syntax that performs the specified operation as
   is appropriate.  A simple 'lazy' map that auto-dereferences values such as promises
   and delays would look like this:

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
