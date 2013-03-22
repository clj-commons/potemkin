;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin.collections
  (:use
    [clojure.walk]
    [potemkin types macros])
  (:import
    java.util.Map$Entry
    clojure.lang.MapEntry
    clojure.lang.IPersistentVector))

(defprotocol PotemkinMap
  (get* [m k default])
  (assoc* [m k v])
  (dissoc* [m k])
  (keys* [m]))

(defn throw-arity [actual]
  `(throw
     (RuntimeException.
       ~(str "Wrong number of args (" actual ")"))))

(eval
  (unify-gensyms
    `(def-abstract-type PotemkinFn []
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


(def-abstract-type AbstractMap []

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

  clojure.lang.Counted

  (count [this]
    (count (potemkin.collections/keys* this)))

  clojure.lang.Seqable
  (seq [this]
    (map #(MapEntry. % (.valAt this % nil)) (potemkin.collections/keys* this)))

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
    (potemkin.collections/get* this k default))

  clojure.lang.Associative
  (containsKey [this k]
    (contains? (.keySet this) k))

  (entryAt [this k]
    (let [v (.valAt this k nil)]
      (reify java.util.Map$Entry
        (getKey [_] k)
        (getValue [_] v))))
  
  (assoc [this k v]
    (potemkin.collections/assoc* this k v))
  
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

   All other necessary functions will be defined so that this behaves like a normal
   Clojure map.  These can be overriden, if desired."
  [name params & body]
  (let [fns '{get get*
              assoc assoc*
              dissoc dissoc*
              keys keys*}
        classname (with-meta (symbol (str (namespace-munge *ns*) "." name)) (meta name))]
    (unify-gensyms
      `(do
         (import
           java.util.Map$Entry
           clojure.lang.MapEntry
           clojure.lang.IPersistentVector)
         (deftype+ ~name ~params ~'potemkin.collections/AbstractMap
           ~@(map
               #(if (sequential? %)
                  (list* (get fns (first %) (first %)) (rest %))
                  %)
               body))
         (defmethod print-method ~classname [o# ~(with-meta `w## {:tag "java.io.Writer"})]
           (.write w## (str o#)))
         ~classname))))

(defmacro def-derived-map
  "Allows a map type to be defined where key-value pairs may be derived from fields.

   For instance, if we want to create a map which contains both upper and lower-case
   versions of a string without immediately instantiating both, we can do this:

   (def-derived-map StringMap [^String s]
     :lower-case (.toLowerCase s)
     :upper-case (.toUpperCase s))

   The resulting map will be have correctly if the defined keys are removed, shadowed,
   etc.

   The above class will automatically create a constructor named '->StringMap'."
  [name params & key-vals]
  (let [key-set (->> key-vals (partition 2) (map first) set)]
    (unify-gensyms
      `(do
       
        (def-map-type ~name ~(vec (conj params `added## `removed##))
         
          (~'get [this# key# default-value#]
            (cond
              (contains? added## key#)
              (get added## key#)
           
              (contains? removed## key#)
              default-value#
           
              :else
              (case key#
                ~@key-vals
                default-value#)))
       
          (~'keys [this#]
            (let [keys# ~key-set
                  keys# (if-not (empty? removed##)
                          (remove #(contains? removed## %) keys#)
                          keys#)
                  keys# (if-not (empty? added##)
                          (set (concat keys# (keys added##)))
                          keys#)]
              keys#))
       
          (~'assoc [this# key# value#]
            (new ~name ~@params (assoc added## key# value#) removed##))
       
          (~'dissoc [this# key#]
            (cond
              (contains? added## key#)
              (new ~name ~@params (dissoc added## key#) removed##)
           
              (contains? ~key-set key#)
              (new ~name ~@params added## (set (conj removed## key#)))
           
              :else
              this#)))

        (defn ~(symbol (str "->" name)) [~@params]
          (new ~name ~@params nil nil))))))
