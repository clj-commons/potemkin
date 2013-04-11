;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin.types
  (:use
    [clojure walk [set :only (union)]]
    [potemkin.macros :only (equivalent? normalize-gensyms safe-resolve)])
  (:require
    [clojure.string :as str]))

;;;

(definterface PotemkinType)

;;;

(defn clean-deftype [x]
  (let [version (let [{:keys [major minor incremental ]} *clojure-version*]
                  (str major "." minor "." incremental))]
    (remove
      #(when-let [min-version (-> % meta :min-version)]
         (neg? (.compareTo version min-version)))
      x)))

;;;

(declare merge-deftypes* deftype->deftype*)

(defn abstract-type? [x]
  (and (symbol? x) (= :potemkin/abstract-type (-> x safe-resolve meta :tag))))

(def ^:dynamic *expanded-types* #{})

(defn expand-deftype [x]
  (let [abstract-types (->> x
                         (filter abstract-type?)
                         (map resolve)
                         (remove *expanded-types*)
                         set)
        abstract-type-bodies (binding [*expanded-types* (union *expanded-types* abstract-types)]
                               (->> abstract-types
                                 (map deref)
                                 (map clean-deftype)
                                 (map expand-deftype)
                                 (map deftype->deftype*)
                                 doall))]
    (apply merge-deftypes*
      (concat
        abstract-type-bodies
        [(deftype->deftype*
           (if (abstract-type? (second x))
             x
             (remove abstract-type? x)))]))))

;;;

(defn transform-deftype*
  [f x]
  (prewalk
    #(if (and (sequential? %) (symbol? (first %)) (= "deftype*" (name (first %))))
       (f %)
       %)
    (macroexpand x)))

(defn deftype->deftype* [x]
  (let [x (macroexpand x)
        find-deftype* (fn find-deftype* [x]
                        (when (sequential? x)
                          (let [f (first x)]
                            (if (and (symbol? f) (= "deftype*" (name f)))
                              x
                              (first (filter find-deftype* x))))))
        remove-nil-implements (fn [x]
                                (concat
                                  (take 5 x)
                                  [(->> (nth x 5) (remove nil?) vec)]
                                  (drop 6 x)))]
    (->> x
      find-deftype*
      remove-nil-implements)))

(defn deftype*->deftype [x]
  (let [[_ name _ params _ implements & body] (deftype->deftype* x)]
    (list* 'deftype name params (concat (remove #{'clojure.lang.IType} implements) body))))

(defn deftype*->fn-map [x]
  (let [fns (drop 6 x)
        fn->key (fn [f] [(first f) (map #(-> % meta :tag) (second f))])]
    (zipmap
      (map fn->key fns)
      fns)))

(defn merge-deftypes*
  ([a]
     a)
  ([a b & rest]
     (let [fns (vals
                 (merge
                   (deftype*->fn-map a)
                   (deftype*->fn-map b)))
           a-implements (nth a 5)
           merged (transform-deftype*
                    #(concat
                       (take 5 %)
                       [(->> (nth % 5) (concat a-implements) distinct vec)]
                       fns)
                    b)]
       (if-not (empty? rest)
         (apply merge-deftypes* merged rest)
         merged))))

;;;

(defmacro def-abstract-type
  "An abstract type, which can be used in conjunction with deftype+."
  [name & body]
  `(def
     ~(with-meta name {:tag :potemkin/abstract-type})
     '(deftype ~name ~@body)))

(defmacro defprotocol+
  "A protocol that won't evaluate if an equivalent protocol with the same name already exists."
  [name & body]
  (let [prev-body (-> name resolve meta :potemkin/body)]
    (when-not (equivalent? prev-body body)
      `(let [p# (defprotocol ~name ~@body)]
         (alter-meta! (resolve p#) assoc :potemkin/body '~(prewalk macroexpand body))
         p#))))

;;;

(def clojure-fn-subs
  [[#"\?"  "_QMARK_"]
   [#"\-"   "_"]
   [#"!"    "_BANG_"]
   [#"\+"   "_PLUS_"]
   [#">"    "_GT_"]
   [#"<"    "_LT_"]
   [#"="    "_EQ_"]
   [#"\*"   "_STAR_"]
   [#"/"    "_SLASH_"]])

(defn munge-fn-name [n]
  (symbol
    (reduce
      (fn [s [regex replacement]]
        (str/replace s regex replacement))
      (name n)
      clojure-fn-subs)))

(defn resolve-tags [n]
  (if-let [tag (-> n meta :tag)]
    (with-meta n (assoc (meta n) :tag (resolve tag)))
    n))

(defmacro definterface+
  "An interface that won't evaluate if an interface with that name already exists.

   Self parameters and multiple arities are defined like defprotocol, as well as wrapping
   functions for each, so it can be used to replace defprotocol seamlessly."
  [name & body]

  (let [fn-names (map first body)
        unrolled-body (mapcat
                        (fn [[fn-name & arg-lists+doc-string]]
                          (let [arg-lists (remove string? arg-lists+doc-string)]
                            (map
                              #(list (munge-fn-name fn-name)
                                 (vec (map resolve-tags (rest %))))
                              arg-lists)))
                        body)]

    `(let [p# ~(if (try
                     (Class/forName (str *ns* "." name))
                     true
                     (catch Exception _
                       false))

                 ;; already exists, just re-import it
                 `(do
                    (import ~(symbol (str *ns* "." name)))
                    nil)

                 ;; define the interface
                 `(definterface
                    ~name
                    ~@unrolled-body))]

       ~@(map
           (fn [[fn-name & arg-lists+doc-string]]
             (let [arg-lists (remove string? arg-lists+doc-string)
                   doc-string (filter string? arg-lists+doc-string)]
               `(defn ~fn-name
                  ~@doc-string
                  ~@(map
                      (fn [args]
                        `(~args
                           (~(symbol (str "." (munge-fn-name fn-name)))
                            ~(with-meta
                               (first args)
                               {:tag (str (ns-name *ns*) "." name)})
                            ~@(rest args))))
                      arg-lists))))
           body)
       
       p#)))

;;;

(defonce type-bodies (atom {}))

(defmacro deftype+
  "A deftype that won't evaluate if an equivalent datatype with the same name already exists,
   and allows abstract types to be used."
  [name params & body]
  (let [body (->> (list* 'deftype name params 'potemkin.types.PotemkinType body)
               clean-deftype
               expand-deftype
               deftype*->deftype)

        classname (with-meta (symbol (str (namespace-munge *ns*) "." name)) (meta name))

        prev-body (@type-bodies classname)]
    
    (when-not (and prev-body
                (equivalent?
                  (transform-deftype* #(drop 3 %) prev-body)
                  (transform-deftype* #(drop 3 %) body)))
      
      (swap! type-bodies assoc classname (prewalk macroexpand body))

      body)))

;;;

(defmacro defrecord+
  "A defrecord that won't evaluate if an equivalent datatype with the same name already exists."
  [name & body]
  (let [classname (with-meta (symbol (str (namespace-munge *ns*) "." name)) (meta name))

        prev-body (@type-bodies classname)]
    
    (when-not (and prev-body
                (equivalent?
                  body
                  prev-body))
      
      (swap! type-bodies assoc classname (prewalk macroexpand body))
      
      `(defrecord ~name ~@body))))

