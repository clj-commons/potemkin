(ns potemkin.template
  (:require
    [clojure.set :as s]
    [riddley.walk :as r]
    [riddley.compiler :as c]))

(defn- validate-body [externs args body]
  (let [valid? (atom true)
        externs (set externs)
        args (set args)
        check? (s/union externs args)]
    (when-not (empty? (s/intersection externs args))
      (throw
        (IllegalArgumentException.
          "No overlap allowed between extern and argument names")))
    (r/walk-exprs
      symbol?
      (fn [s]
        (when (and (check? s) (->> (c/locals) keys (filter #(= s %)) first meta ::valid not))
          (throw
            (IllegalArgumentException.
              (str \' s \' " is shadowed by local lexical binding"))))
        (when-not (get (c/locals) s)
          (throw
            (IllegalArgumentException.
              (str \' s \' " is undefined, must be explicitly defined as an extern."))))
        s)
      `(let [~@(mapcat
                 (fn [x]
                   [(with-meta x {::valid true}) nil])
                 (concat
                   externs
                   args))]
         ~body))
    true))

(defn- unquote? [x]
  (and (seq? x) (= 'clojure.core/unquote (first x))))

(defn- splice? [x]
  (and (seq? x) (= 'clojure.core/unquote-splicing (first x))))

(defn validate-externs [name externs]
  (doseq [e externs]
    (when-not (contains? (c/locals) e)
      (throw
        (IllegalArgumentException.
          (str "template "
            \' name \'
            " expects extern "
            \' e \'
            " to be defined within local scope."))))))

(defmacro deftemplate [name externs args & body]
  (let [body `(do ~@body)]
    (validate-body externs args body)
    (let [arg? (set args)
          pred (fn [x] (or (seq? x) (vector? x) (symbol? x)))]
      (list 'defmacro name args
        (list 'validate-externs (list 'quote name) (list 'quote (set externs)))
        (r/walk-exprs
          pred
          (fn this [x]
            (if (or (seq? x) (vector? x))
              (let [splicing? (some splice? x)
                    terms (map
                            (fn [t]
                              (cond
                                (unquote? t) (second t)
                                (splice? t) t
                                :else (r/walk-exprs pred this t)))
                            x)
                    x' (if (some splice? x)
                         (list* 'concat (map #(if (splice? %) (second %) [%]) terms))
                         (list* 'list terms))]
                (if (vector? x)
                  (vec x')
                  x'))
              (cond
                (arg? x)
                x

                (arg? (-> x meta :tag))
                (list 'quote
                  (list 'with-meta x
                    (list 'assoc
                      (list 'meta x)
                      {:tag (-> x meta :tag)})))

                :else
                (list 'quote x))))
          body)))))
