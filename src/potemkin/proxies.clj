(ns potemkin.proxies
  (:require
    [clojure.set :as set]))

(defmacro def-map-type
  "Like deftype, but must contain definitions for the following functions:

   (get [this key default-value])
   (assoc [this key value])
   (dissoc [this key])
   (key-set [this])
   (empty [this])

   and optionally

   (equals [this other])
   (hash [this])

   All other necessary functions will be defined so that this behaves like a normal
   Clojure map.  A `->Name` constructor will automatically be defined, and should be used
   instead of the `Name.` constructor."
  [name params & body]
  (let [imap '#{get assoc dissoc key-set empty}
        iequals '#{equals hash}
        fn-names '{equals eq
                   key-set keySet}
        keys (->> body (map first) set)
        body (map
               #(list*
                  (if-let [fn (fn-names (first %))]
                    fn
                    (first %))
                  (rest %))
               body)]

    (when-let [missing (seq (set/difference imap keys))]
      (throw (IllegalArgumentException. (str "missing functions: " (pr-str missing)))))

    (when (= 1 (count (set/difference iequals keys)))
      (throw (IllegalArgumentException. (str "missing function: " (pr-str (set/difference iequals keys))))))

    `(do
       (deftype ~name ~params
         ~@(when (seq (set/intersection iequals keys))
             `(potemkin.PersistentMapProxy$IEquality))
         ~@(when (seq (set/intersection imap keys))
             `(potemkin.PersistentMapProxy$IMap))
         ~@body)
       (alter-var-root (resolve (symbol ~(str "->" name)))
         (fn [f#]
           (fn [~@params]
             (potemkin.PersistentMapProxy.
               (new ~name ~@params))))))))

(defmacro def-derived-map
  [name params & {:as m}]
  (let [interface (symbol (str "ILookup" name))
        added (with-meta (gensym "added") {:tag "java.util.Map"})
        removed-bitset (with-meta (gensym "removed-bitset") {:tag "java.util.BitSet"})
        added-bitset (with-meta (gensym "added-bitset") {:tag "java.util.BitSet"})
        methods (->> (count m) range (map #(symbol (str "get__" (munge %)))))]
    `(do
       (definterface ~interface
         ~@(map
             #(list % [])
             methods))

       (deftype ~name ~(vec (concat params [added removed-bitset added-bitset]))

         potemkin.PersistentMapProxy$IMap
         ~interface

         ~@(->> (map vector methods (vals m))
             (map
               (fn [[name f]]
                 (list name `[_#] f))))

         (~'get [~'this ~'k ~'default-value]
           (case ~'k
             ~@(->> (map vector (range) methods m)
                 (map
                   (fn [[i method [k v]]]
                     `(cond
                        (.get ~removed-bitset ~i) ~'default-value
                        (.get ~added-bitset ~i)   (get ~added ~'k ~'default-value)
                        :else                     (~(symbol (str "." method)) ~'this))))
                 (interleave (keys m)))
             (get ~added ~'k ~'default-value)))

         (~'keySet [_#]
           (let [~(with-meta 'set {:tag "java.util.HashSet"}) (java.util.HashSet.)]
             ~@(->> (map vector (range) (keys m))
                 (map
                   (fn [[i k]]
                     `(when-not (.get ~removed-bitset ~i)
                        (.add ~'set ~k)))))
             (.addAll ~'set (.keySet ~added))
             ~'set))

         (~'empty [_#]
           {})

         (~'assoc [_# ~'k ~'v]
           (case ~'k
             ~@(->> (map vector (range) methods m)
                 (map
                   (fn [[i method [k v]]]
                     `(let [removed-bitset# (doto ^java.util.BitSet (.clone ~removed-bitset) (.set ~i false))
                            added-bitset# (doto ^java.util.BitSet (.clone ~added-bitset) (.set ~i true))]
                        (new ~name ~@params (assoc ~added ~'k ~'v) removed-bitset# added-bitset#))))
                 (interleave (keys m)))
             (new ~name ~@params (assoc ~added ~'k ~'v) ~removed-bitset ~added-bitset)))

         (~'dissoc [_# ~'k]
           (case ~'k
             ~@(->> (map vector (range) methods m)
                 (map
                   (fn [[i method [k v]]]
                     `(let [removed-bitset# (doto ^java.util.BitSet (.clone ~removed-bitset) (.set ~i true))
                            added-bitset# (doto ^java.util.BitSet (.clone ~added-bitset) (.set ~i false))]
                        (new ~name ~@params (dissoc ~added ~'k) removed-bitset# added-bitset#))))
                 (interleave (keys m)))
             (new ~name ~@params (dissoc ~added ~'k) ~removed-bitset ~added-bitset))))

       (alter-var-root (resolve (symbol ~(str "->" name)))
         (fn [f#]
           (fn [~@params]
             (potemkin.PersistentMapProxy.
               (new ~name ~@params {} (java.util.BitSet. ~(count m)) (java.util.BitSet. ~(count m)) ))))))))
