(ns potemkin.macros-test
  (:use
    [clojure test]
    [potemkin utils]))

(deftest test-condp-case
  (let [f #(condp-case identical? %
             (:abc :def) :foo
             :xyz :bar)]
    (is (= :foo (f :abc) (f :def)))
    (is (= :bar (f :xyz)))))

(deftest test-try*
  )
