;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin.macros
  (:use [clojure walk]))

(defn safe-resolve [x]
  (try
    (resolve x)
    (catch Exception _
      nil)))

(def unified-gensym-regex #"([a-zA-Z0-9\-]+)#__\d+__auto__$")
(def gensym-regex #"([a-zA-Z0-9\-]+)#?__\d+(__auto__)?$")

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
         (with-meta
           (symbol (str (gensym* (str (un-gensym %) "__")) "__auto__"))
           (meta %))
         %)
      body)))

(defn normalize-gensyms
  [body]
  (let [cnt (atom 0)
        gensym* #(str % "__norm__" (swap! cnt inc))]
    (postwalk
      #(if (gensym? %)
         (with-meta
           (symbol (gensym* (un-gensym %)))
           (meta %))
         %)
      body)))

(defn equivalent?
  [a b]
  (if-not (and a b)
    (= a b)
    (=
     (->> a (prewalk macroexpand) normalize-gensyms)
     (->> b (prewalk macroexpand) normalize-gensyms))))

(defmacro fast-bound-fn
  "Creates a variant of bound-fn which doesn't assume you want a merged
   context between the source and execution environments."
  [& fn-body]
  (let [{:keys [major minor]} *clojure-version*
        use-thread-bindings? (and (= 1 major) (< minor 3))
        use-get-binding? (and (= 1 major) (< minor 4))]
    (if use-thread-bindings?
      `(let [bindings# (get-thread-bindings)
             f# (fn ~@fn-body)]
         (fn [~'& args#]
           (with-bindings bindings#
             (apply f# args#))))      
      `(let [bound-frame# ~(if use-get-binding?
                             `(clojure.lang.Var/getThreadBindingFrame)
                             `(clojure.lang.Var/cloneThreadBindingFrame))
             f# (fn ~@fn-body)]
         (fn [~'& args#]
           (let [curr-frame# (clojure.lang.Var/getThreadBindingFrame)]
             (clojure.lang.Var/resetThreadBindingFrame bound-frame#)
             (try
               (apply f# args#)
               (finally
                 (clojure.lang.Var/resetThreadBindingFrame curr-frame#)))))))))

(defn fast-bound-fn*
  "Creates a function which conveys bindings, via fast-bound-fn."
  [f]
  (fast-bound-fn [& args]
    (apply f args)))

(defmacro try*
  "A variant of try that is fully transparent to transaction retry exceptions"
  [& body+catch]
  (let [body (take-while
               #(or (not (sequential? %)) (not (= 'catch (first %))))
               body+catch)
        catch (drop (count body) body+catch)
        ignore-retry (fn [x]
                       (when x
                         (let [ex (nth x 2)]
                           `(~@(take 3 x)
                             (if (lamina.core.utils/retry-exception? ~ex)
                               (throw ~ex)
                               (do ~@(drop 3 x)))))))
        class->clause (-> (zipmap (map second catch) catch)
                        (update-in ['Throwable] ignore-retry)
                        (update-in ['Error] ignore-retry))]
    `(try
       ~@body
       ~@(->> class->clause vals (remove nil?)))))

(defmacro condp-case
  "A variant of condp which has case-like syntax for options.  When comparing
   smaller numbers of keywords, this can be faster, sometimes significantly."
  [predicate value & cases]
  (unify-gensyms
    `(let [val## ~value
           pred## ~predicate]
       (cond
         ~@(->> cases
             (partition 2)
             (map
               (fn [[vals expr]]
                 `(~(if (sequential? vals)
                      `(or ~@(map (fn [x] `(pred## val## ~x)) vals))
                      `(pred## val## ~vals))
                   ~expr)))
             (apply concat))
         :else
         ~(when-not (even? (count cases))
            `(throw (IllegalArgumentException. (str "no matching clause for " (pr-str val##))))
            (last cases))))))

