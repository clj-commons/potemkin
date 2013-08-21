(ns potemkin.macros
  (:use [potemkin.walk]))

(defn macroexpand+
  "Expands both macros and inline functions."
  [x]
  (let [x* (macroexpand x)]
    (if-let [inline-fn (and (seq? x*)
                         (symbol? (first x*))
                         (not (-> x* meta ::transformed))
                         (-> x first resolve meta :inline))]
      (let [x** (apply inline-fn (rest x*))]
        (recur
          ;; unfortunately, static function calls can look a lot like what we just
          ;; expanded, so prevent infinite expansion
          (if (= '. (first x**))
            (concat (butlast x**) [(with-meta (last x**) {::transformed true})])
            x**)))
      x*)))

(defn safe-resolve [x]
  (try
    (resolve x)
    (catch Exception _
      nil)))

(def unified-gensym-regex #"([a-zA-Z0-9\-\'\*]+)#__\d+__auto__$")

(def gensym-regex #"(_|[a-zA-Z0-9\-\'\*]+)#?_+(\d+_*#?)+(auto__)?$")

(defn unified-gensym? [s]
  (and
    (symbol? s)
    (re-find unified-gensym-regex (str s))))

(defn gensym? [s]
  (and
    (symbol? s)
    (re-find gensym-regex (str s))))

(defn un-gensym [s]
  (second (re-find gensym-regex (str s))))

(defn unify-gensyms
  "All gensyms defined using two hash symbols are unified to the same
   value, even if they were defined within different syntax-quote scopes."
  [body]
  (let [gensym* (memoize gensym)]
    (postwalk
      #(if (unified-gensym? %)
         (symbol (str (gensym* (str (un-gensym %) "__")) "__auto__"))
         %)
      body)))

(defn normalize-gensyms
  [body]
  (let [cnt (atom 0)
        gensym* #(str % "__norm__" (swap! cnt inc))]
    (postwalk
      #(if (gensym? %)
         (symbol (gensym* (un-gensym %)))
         %)
      body)))

(defn equivalent?
  [a b]
  (if-not (and a b)
    (= a b)
    (=
      (->> a (map (partial prewalk macroexpand)) normalize-gensyms)
      (->> b (map (partial prewalk macroexpand)) normalize-gensyms))))





