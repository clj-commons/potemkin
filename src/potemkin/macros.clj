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
  (=
    (->> a (prewalk macroexpand) normalize-gensyms)
    (->> b (prewalk macroexpand) normalize-gensyms)))
