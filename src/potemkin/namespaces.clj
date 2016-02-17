(ns potemkin.namespaces)

(defn link-vars
  "Makes sure that all changes to `src` are reflected in `dst`."
  [src dst]
  (add-watch src dst
    (fn [_ src old new]
      (alter-var-root dst (constantly @src))
      (alter-meta! dst merge (dissoc (meta src) :name)))))

(defmacro import-fn
  "Given a function in another namespace, defines a function with the
   same name in the current namespace.  Argument lists, doc-strings,
   and original line-numbers are preserved."
  ([sym]
     `(import-fn ~sym nil))
  ([sym name]
     (let [vr (resolve sym)
           m (meta vr)
           n (or name (:name m))
           arglists (:arglists m)
           protocol (:protocol m)]
       (when-not vr
         (throw (IllegalArgumentException. (str "Don't recognize " sym))))
       (when (:macro m)
         (throw (IllegalArgumentException.
                  (str "Calling import-fn on a macro: " sym))))

       `(do
          (def ~(with-meta n {:protocol protocol}) (deref ~vr))
          (alter-meta! (var ~n) merge (dissoc (meta ~vr) :name))
          (link-vars ~vr (var ~n))
          ~vr))))

(defmacro import-macro
  "Given a macro in another namespace, defines a macro with the same
   name in the current namespace.  Argument lists, doc-strings, and
   original line-numbers are preserved."
  ([sym]
     `(import-macro ~sym nil))
  ([sym name]
     (let [vr (resolve sym)
           m (meta vr)
           n (or name (with-meta (:name m) {}))
           arglists (:arglists m)]
       (when-not vr
         (throw (IllegalArgumentException. (str "Don't recognize " sym))))
       (when-not (:macro m)
         (throw (IllegalArgumentException.
                  (str "Calling import-macro on a non-macro: " sym))))
       `(do
          (def ~n ~(resolve sym))
          (alter-meta! (var ~n) merge (dissoc (meta ~vr) :name))
          (.setMacro (var ~n))
          (link-vars ~vr (var ~n))
          ~vr))))

(defmacro import-def
  "Given a regular def'd var from another namespace, defined a new var with the
   same name in the current namespace."
  ([sym]
     `(import-def ~sym nil))
  ([sym name]
     (let [vr (resolve sym)
           m (meta vr)
           n (or name (:name m))
           n (with-meta n (if (:dynamic m) {:dynamic true} {}))
           nspace (:ns m)]
       (when-not vr
         (throw (IllegalArgumentException. (str "Don't recognize " sym))))
       `(do
          (def ~n @~vr)
          (alter-meta! (var ~n) merge (dissoc (meta ~vr) :name))
          (link-vars ~vr (var ~n))
          ~vr))))

(defn- unravel* [ns x]
  (let [[sym new-name] (if (sequential? x)
                         x
                         [x x])]
       [(symbol
         (str ns
              (when-let [n (namespace sym)]
                (str "." n)))
         (name sym))
        new-name]))

(defn- unravel [x]
  (if (sequential? x)
    (->> x
         rest
         (map #(unravel* (first x) %)))
    [[x x]]))

(defmacro import-vars
  "Imports a list of vars from other namespaces."
  [& syms]
  (let [syms (mapcat unravel syms)]
    `(do
       ~@(map
          (fn [[sym new-name]]
            (let [vr (resolve sym)
                  m (meta vr)]
              (cond
               (:macro m) `(import-macro ~sym ~new-name)
               (:arglists m) `(import-fn ~sym ~new-name)
               :else `(import-def ~sym ~new-name))))
          syms))))

