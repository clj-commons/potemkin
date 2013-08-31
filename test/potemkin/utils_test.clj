(ns potemkin.utils-test
  (:use
    [clojure test]
    [potemkin utils])
  (:require
    [criterium.core :as c]))

(deftest test-condp-case
  (let [f #(condp-case identical? %
             (:abc :def) :foo
             :xyz :bar)]
    (is (= :foo (f :abc) (f :def)))
    (is (= :bar (f :xyz)))))

(deftest test-try*
  )

(deftest ^:benchmark benchmark-fast-memoize
  (let [f (memoize +)
        f' (fast-memoize +)]
    (println "\n normal memoize \n")
    (c/quick-bench
      (f 1))
    (c/quick-bench
      (f 1 2))
    (c/quick-bench
      (f 1 2 3))
    (c/quick-bench
      (f 1 2 3 4))
    (c/quick-bench
      (f 1 2 3 4 5))

    (println "\n fast memoize \n")
    (c/quick-bench
      (f' 1))
    (c/quick-bench
      (f' 1 2))
    (c/quick-bench
      (f' 1 2 3))
    (c/quick-bench
      (f' 1 2 3 4))
    (c/quick-bench
      (f' 1 2 3 4 5))))
