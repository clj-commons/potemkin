(ns potemkin.types-test
  (:use
    [clojure test]
    [potemkin]))

(deftest test-defrecord+
  (is (not= nil (eval '(potemkin/defrecord+ FooR [x y]))))
  (is (= nil (eval '(potemkin/defrecord+ FooR [x y]))))
  (is (not= nil (eval '(potemkin/defrecord+ FooR [x y z])))))

(deftest test-deftype+
  (is (not= nil (eval '(potemkin/deftype+ FooT [x y]))))
  (is (= nil (eval '(potemkin/deftype+ FooT [x y]))))
  (is (not= nil (eval '(potemkin/deftype+ FooT [x y z])))))

(deftest test-defprotocol+
  (is (not= nil (eval '(potemkin/defprotocol+ BarP (bar [x y])))))
  (is (= nil (eval '(potemkin/defprotocol+ BarP (bar [x y])))))
  (is (not= nil (eval '(potemkin/defprotocol+ BarP (bar [x y z]))))))

(deftest test-empty-defprotocol+-body
  (let [prot-sym (gensym)]
    (binding [*ns* (the-ns 'potemkin.types-test)]
      (eval (list 'potemkin/defprotocol+ prot-sym))
      (is (resolve prot-sym)))))

(deftest test-definterface+
  (is (not= nil (eval '(potemkin/definterface+ IBar (bar-baz [x y])))))
  (is (= nil (eval '(potemkin/definterface+ IBar (bar-baz [x y])))))
  (is (= nil (eval '(potemkin/definterface+ IBar (bar-baz [x y z]))))))

(definterface+ ITest
  (test-fn ^long [_ ^long x])
  (self [_]))

(deftype+ TestType [^long n]
  ITest
  (test-fn [_ x] (+ n x))
  (self [this] this))

(deftest test-primitive-interface
  (is (= 6
        (test-fn (TestType. 1) 5)
        (apply test-fn (TestType. 1) [5])
        (test-fn (self (self (TestType. 1))) (test-fn (TestType. 1) 4)))))

(defprotocol+ Foo
  (foo [_]))

(defprotocol+ Bar
  (bar [_]))

(extend-protocol+ Foo
  ITest
  (foo [this] (self this))

  Bar
  (foo [this] (bar this)))

(extend-protocol+ Bar
  Foo
  (bar [this] (foo this)))

(deftest test-extend-protocol+
  (is (= 1 (test-fn (foo (TestType. 1)) 0)))
  (is (= 1 (test-fn (bar (TestType. 1)) 0))))
