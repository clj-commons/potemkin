;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin.test.types
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
  (test-fn ^long [_ ^long x]))

(deftype+ TestType [^long n]
  ITest
  (test-fn [_ x] (+ n x)))

(deftest test-primitive-interface
  (is (= 6 (test-fn (TestType. 1) 5)))
  (is (= 6 (apply test-fn (TestType. 1) [5]))))
