;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin.test.macros
  (:use
    [clojure test]
    [potemkin macros]))

(defn simple-unify-form []
  (unify-gensyms
    `(let [cnt## (atom 0)]
       ~@(map
           (fn [_] `(swap! cnt## inc))
           (range 100))
       cnt##)))

(deftest test-unify-form
  (is (= 100 @(eval (simple-unify-form)))))

(declare foo)

(defn test-transform-defn [form & arities]
  (in-ns 'potemkin.test.macros)
  (eval (transform-defn-bodies (constantly [1]) form))
  (doseq [a arities]
    (is (= 1 (apply foo (repeat a nil))))))

(deftest test-transform-defn-bodies
  (test-transform-defn `(defn foo [x#])
    1)
  (test-transform-defn `(defn foo [])
    0)
  (test-transform-defn `(defn foo "doc-string" {:abc :def} ([]) ([x#]) ([x# y#]))
    0 1 2))

(defn test-transform-fn [form & arities]
  (let [f (eval (transform-fn-bodies (constantly [1]) form))]
    (doseq [a arities]
      (is (= 1 (apply f (repeat a nil)))))))

(deftest test-transform-fn-bodies
  (test-transform-fn `(fn [x#])
    1)
  (test-transform-fn `(fn foo [x#])
    1)
  (test-transform-fn `(fn foo [])
    0)
  (test-transform-fn `(fn foo ([]) ([x#]) ([x# y#]))
    0 1 2))
