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

     `(let [vr# (resolve '~sym)]
        (def ~(with-meta n {:protocol protocol}) (deref vr#))
        (alter-meta! (var ~n) merge (dissoc (meta vr#) :name))
        (link-vars vr# (var ~n))
        vr#))))

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
     `(let [vr# (resolve '~sym)]
        (def ~n (deref vr#))
        (alter-meta! (var ~n) merge (dissoc (meta vr#) :name))
        (.setMacro (var ~n))
        (link-vars vr# (var ~n))
        vr#))))

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
     `(let [vr# (resolve '~sym)]
        (def ~n (deref vr#))
        (alter-meta! (var ~n) merge (dissoc (meta vr#) :name))
        (link-vars vr# (var ~n))
        vr#))))

(defn- unravel* [x]
  (if (sequential? x)
    (->> x
         rest
         (mapcat unravel*)
         (map
          #(symbol
            (str (first x)
                 (when-let [n (namespace %)]
                   (str "." n)))
            (name %))))
    [x]))

(defn- unravel [x]
  (or (and (sequential? x)
           (let [[ns refer-kw refers rename-kw renames] x]
             (when (and (= :refer refer-kw)
                        (sequential? refers)
                        (or (and (nil? rename-kw) (nil? renames))
                            (and (= :rename rename-kw) (map? renames))))
               (map #(with-meta (symbol (str ns) (name %)) {:name (get renames %)})
                    refers))))
      (unravel* x)))

(defmacro import-vars
  "Imports a list of vars from other namespaces."
  [& syms]
  (let [syms (mapcat unravel syms)]
    `(do
       ~@(map
          (fn [sym]
            (let [vr (resolve sym)
                  m (meta vr)
                  ?name (-> sym meta :name)]
              (cond
                (nil? vr) `(throw (ex-info (format "`%s` does not exist" '~sym) {}))
                (:macro m) `(import-macro ~sym ~?name)
                (:arglists m) `(import-fn ~sym ~?name)
                :else `(import-def ~sym ~?name))))
          syms))))
