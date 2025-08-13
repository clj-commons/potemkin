(ns potemkin.types
  (:use
    [clojure [set :only (union)]]
    [potemkin.macros :only (equivalent? normalize-gensyms safe-resolve unify-gensyms)])
  (:require
    [riddley.walk :as r]
    [clojure.set :as set]
    [clojure.string :as str]))

;;;

(definterface PotemkinType)

;;;

(defn protocol? [x]
  (and
    (contains? x :on-interface)
    (class? (:on-interface x))))

(defn- extend-implementations [proto impls body]
  (let [proto-val @(resolve proto)
        impls (remove
                #(or
                   (= (:on-interface proto-val) %)
                   (contains? (:impls proto-val) %))
                impls)]
    (eval
      `(extend-protocol ~proto
         ~@(apply concat
             (interleave (map list impls) (repeat body)))))))

(defn- register-impl-callback [proto-var callback]
  (add-watch proto-var callback
    (fn [_ proto-var {old-impls :impls} {new-impls :impls}]
      (callback (set/difference (set (keys new-impls)) (set (keys old-impls)))))))

(defmacro extend-protocol+
  "A variant of `extend-protocol` that allows `proto` to be extended over other protocols, as well as classes and `nil`."
  [proto & body]
  (let [split-on-symbol (fn this [[sym & rest :as s]]
                          (when-not (empty? s)
                            (lazy-seq
                              (cons
                                (cons sym (take-while (complement symbol?) rest))
                                (this (drop-while (complement symbol?) rest))))))
        decls (split-on-symbol body)
        protocol? (fn [[sym]]
                    (let [x (resolve sym)]
                      (and (var? x) (protocol? @x))))
        protos (filter protocol? decls)
        classes (remove protocol? decls)]

    (doseq [[target-proto & body] protos]
      (let [target-proto-var (resolve target-proto)]

        ;; all future implementations should be extended
        (register-impl-callback target-proto-var
          (fn [new-impls]
            (extend-implementations proto new-impls body)))

        ;; all current implementations should be extended
        (let [{:keys [on-interface impls]} @target-proto-var]
          (extend-implementations proto (cons on-interface (keys impls)) body))))

    `(extend-protocol ~proto
       ~@(apply concat classes))))

;;;

(defn clean-deftype [x]
  (let [version (let [{:keys [major minor incremental ]} *clojure-version*]
                  (str major "." minor "." incremental))]
    (remove
      #(when-let [min-version (-> % meta :min-version)]
         (neg? (.compareTo version min-version)))
      x)))

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
  (r/walk-exprs
    #(and (sequential? %) (= 'deftype* (first %)))
    f
    x))

(defn deftype->deftype* [x]
  (let [x (r/macroexpand x)
        find-deftype* (fn find-deftype* [x]
                        (when (sequential? x)
                          (let [f (first x)]
                            (if (= 'deftype* f)
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
  (let [[_ dname _ params _ implements & body] (deftype->deftype* x)]
    (list* 'deftype (symbol (name dname)) params (concat (remove #{'clojure.lang.IType} implements) body))))

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
     '(deftype ~name [] ~@body)))

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
  (with-meta
    (symbol
      (reduce
        (fn [s [regex replacement]]
          (str/replace s regex replacement))
        (name n)
        clojure-fn-subs))
    (meta n)))

(defn resolve-tag [n]
  (if-let [tag (-> n meta :tag)]
    (with-meta n
      (assoc (meta n)
        :tag (or
               (#{'long 'double 'short 'int 'byte 'boolean 'void} tag)
               (resolve tag))))
    n))

(defn untag [n]
  (with-meta n (dissoc (meta n) :tag)))

;;;

(defonce type-bodies (atom {}))

(defmacro deftype+
  "A deftype that allows abstract types to be used."
  [name params & body]
  (let [body (->> (list* 'deftype name params 'potemkin.types.PotemkinType body)
                  clean-deftype
                  expand-deftype
                  deftype*->deftype)

        classname (with-meta (symbol (str (namespace-munge *ns*) "." name)) (meta name))]

    (swap! type-bodies assoc classname
           (r/macroexpand-all body))

    body))

(defmacro reify+
  "A reify that supports abstract types."
  [& body]
  (let [body (->> (list* 'deftype (gensym "reify") [] 'potemkin.types.PotemkinType body)
               clean-deftype
               expand-deftype
               deftype*->deftype)]

    `(reify ~@(->> body (drop 3) (remove #{'clojure.lang.IObj clojure.lang.IObj})))))
