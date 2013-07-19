;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

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
