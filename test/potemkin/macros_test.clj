(ns potemkin.macros-test
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
