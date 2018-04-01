Potemkin is a collection of facades and workarounds for things that are more difficult than they should be.  All functions are within the `potemkin` namespace.

### usage

[![Build Status](https://travis-ci.org/ztellman/potemkin.png?branch=master)](https://travis-ci.org/ztellman/potemkin)

```clj
[potemkin "0.4.5"]
```

### `import-vars`

Clojure namespaces conflate the layout of your code and your API.  For larger libraries, this generally means that you either have large namespaces (e.g. clojure.core) or a large number of namespaces that have to be used in concert to accomplish non-trivial tasks (e.g. Ring).

The former approach places an onus on the creator of the library; the various orthogonal pieces of his library all coexist, which can make it difficult to keep everything straight. The latter approach places an onus on the consumers of the library, forcing them to remember exactly what functionality resides where before they can actually use it.

`import-vars` allows functions, macros, and values to be defined in one namespace, and exposed in another.  This means that the structure of your code and the structure of your API can be decoupled.

```clj
(import-vars
  [clojure.walk
    prewalk
    postwalk]
  [clojure.data
    diff])
```

### `def-map-type`

A Clojure map implements the following interfaces: `clojure.lang.IPersistentCollection`, `clojure.lang.IPersistentMap`, `clojure.lang.Counted`, `clojure.lang.Seqable`, `clojure.lang.ILookup`, `clojure.lang.Associative`, `clojure.lang.IObj`, `java.lang.Object`, `java.util.Map`, `java.util.concurrent.Callable`, `java.lang.Runnable`, and `clojure.lang.IFn`.  Between them, there's a few dozen functions, many with overlapping functionality, all of which need to be correctly implemented.

Despite this, there are only six functions which really matter: `get`, `assoc`, `dissoc`, `keys`, `meta`, and `with-meta`.  `def-map-type` is a variant of `deftype` which, if those six functions are implemented, will look and act like a Clojure map.

For instance, here's a map which will automatically realize any delays, allowing for lazy evaluation semantics:

```clj
(def-map-type LazyMap [m mta]
  (get [_ k default-value]
    (if (contains? m k)
      (let [v (get m k)]
        (if (instance? clojure.lang.Delay v)
          @v
          v))
      default-value))
  (assoc [_ k v]
    (LazyMap. (assoc m k v) mta))
  (dissoc [_ k]
     (LazyMap. (dissoc m k) mta))
  (keys [_]
    (keys m))
  (meta [_]
    mta)
  (with-meta [_ mta]
    (LazyMap. m mta)))
```

### `def-derived-map`

Often a map is just a view onto another object, especially when dealing with Java APIs.  While we can create a function which converts it into an entirely separate object, for both performance and memory reasons it can be useful to create a map which simply acts as a delegate to the underlying objects:

```clj
(def-derived-map StringProperties [^String s]
  :base s
  :lower-case (.toLowerCase s)
  :upper-case (.toUpperCase s))
```

Each time the key `:lower-case` is looked up, it will invoke `.toLowerCase.  The resulting datatype behaves exactly like a normal Clojure map; new keys can be added and derived keys can be removed.

### `def-abstract-type` and `deftype+`

The reason it's so laborious to define a map-like data structure is because the implementation cannot be shared between different types.  For instance, `clojure.lang.ISeq` has both `next` and `more` methods.  However, while `more` can be implemented in terms of `next`, as it is in [clojure.lang.ASeq](https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/ASeq.java#L129), within Clojure it must be reimplemented anew for each new type.

However, using `def-abstract-type`, we can avoid this:

```clj
(def-abstract-type ASeq
  (more [this]
    (let [n (next this)]
      (if (empty? n)
        '()
        n)))))
```

This abstract type may be used within the body of `deftype+`, which is just like a vanilla `deftype` except for the support for abstract types.

```clj
(deftype+ CustomSeq [s]
  ASeq
  clojure.lang.ISeq
  (seq [_] s)
  (cons [_ x] (CustomSeq. (cons x s)))
  (next [_] (CustomSeq. (next s))))
```

### `definterface+`

Every method on a type must be defined within a protocol or an interface.  The standard practice is to use `defprotocol`, but this imposes a certain overhead in both [time and memory](https://gist.github.com/ztellman/5603216).  Furthermore, protocols don't support primitive arguments.  If you need the extensibility of protocols, then there isn't another option, but often interfaces suffice.

While `definterface` uses an entirely different convention than `defprotocol`, `definterface+` uses the same convention, and automatically defines inline-able functions which call into the interface.  Thus, any protocol which doesn't require the extensibility can be trivially turned into an interface, with all the inherent savings.

### `unify-gensyms`

Gensyms enforce hygiene within macros, but when quote syntax is nested, they can become a pain.  This, for instance, doesn't work:

```clj
`(let [x# 1]
   ~@(map
       (fn [n] `(+ x# ~n))
       (range 3)))
```

Because `x#` is going to expand to a different gensym in the two different contexts.  One way to work around this is to explicitly create a gensym ourselves:

```clj
(let [x-sym (gensym "x")]
  `(let [~x-sym 1]
     ~@(map
         (fn [n] `(+ ~x-sym ~n))
         (range 3))))
```

However, this is pretty tedious, since we may need to define quite a few of these explicit gensym names.  Using `unify-gensyms`, however, we can rely on the convention that any var with two hashes at the end should be unified:

```clj
(unify-gensyms
  `(let [x## 1]
     ~@(map
         (fn [n] `(+ x## ~n))
         (range 3)))
```

### `fast-bound-fn` and `fast-memoize`

Variants of Clojure's `bound-fn` and `memoize` which are significantly faster.

### License

Copyright © 2013 Zachary Tellman

Distributed under the [MIT License](http://opensource.org/licenses/MIT).  This means that pieces of this library may be copied into other libraries if they don't wish to have this as an explicit dependency, as long as it is credited within the code.
