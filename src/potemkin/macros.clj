;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin.macros
  (:use [clojure walk]))

(def gensym-regex #"([a-zA-Z\-]+)#__\d+__auto__$")

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
      #(if (gensym? %)
         (symbol (str (gensym* (str (un-gensym %) "__")) "__auto__"))
         %)
      body)))

(defn transform-defn-bodies
  "Takes a (defn ...) form, and transform the bodies. The transform function is
   passed the arglist, the function metadata, and the function body."
  [f form]
  (let [form (macroexpand form)
        fn-form (->> form (drop 2) first)
        fn-form (if (= ".withMeta" (str (first fn-form)))
                  (second fn-form)
                  fn-form)
        fn-form (macroexpand fn-form)
        fn-name (second form)
        arity-forms (->> fn-form (drop-while symbol?))
        arity-forms (map
                      (fn [arity-form]
                        (let [args (first arity-form)]
                          `(~args ~@(f args (rest arity-form)))))
                      arity-forms)]
    `(~(first form)
      ~(second form)
      (fn* ~@arity-forms))))
