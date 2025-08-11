(ns potemkin.types-test
  (:require
    [clojure.test :refer :all]
    [potemkin.types :refer :all]))

(definterface ITest
  (testfn [x])
  (self []))

(deftype+ TestType [^long n]
  ITest
  (testfn [_ x] (+ n x))
  (self [this] this))

(deftest test-primitive-interface
  (is (= 6
         (.testfn (TestType. 1) 5)
         (.testfn (.self (.self (TestType. 1))) (.testfn (TestType. 1) 4)))))
