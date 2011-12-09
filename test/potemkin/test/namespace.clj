;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin.test.namespace
  (:use
    potemkin
    clojure.test)
  (:require
    [clojure.repl :as repl]
    [clojure.string :as str]
    [potemkin.test.protocol :as p]))

(import-macro #'repl/source)
(import-macro #'repl/doc)
(import-fn #'repl/find-doc)
(import-fn #'p/multi-arity)
(import-fn #'p/protocol-function)

(defn drop-lines [n s]
  (->> s str/split-lines (drop n) (interpose "\n") (apply str)))

(defmacro out= [& args]
  `(= ~@(map (fn [x] `(with-out-str ~x)) args)))

(defmacro rest-out= [& args]
  `(do
     ;;(do ~@(map (fn [x] `(println (with-out-str ~x))) args))
     (= ~@(map (fn [x] `(drop-lines 2 (with-out-str ~x))) args))))

(deftest test-import-macro
  (is (out= (source repl/source) (source source)))
  (is (rest-out= (doc repl/doc) (doc doc))))

(deftest test-import-fn
  (is (out= (source repl/find-doc) (source find-doc)))
  (is (rest-out= (doc repl/find-doc) (doc find-doc)))
  (is (out= (source p/multi-arity) (source multi-arity)))
  (is (rest-out= (doc p/multi-arity) (doc multi-arity)))
  (is (rest-out= (doc p/protocol-function) (doc protocol-function)))
  )


