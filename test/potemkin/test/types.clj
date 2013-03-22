;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin.test.collections
  (:use
    [clojure test]
    [potemkin]))

(deftest test-defrecord+
  (is (not= nil (eval '(defrecord+ Foo [x y]))))
  (is (= nil (eval '(defrecord+ Foo [x y]))))
  (is (not= nil (eval '(defrecord+ Foo [x y z])))))

(deftest test-deftype+
  (is (not= nil (eval '(deftype+ Foo [x y]))))
  (is (= nil (eval '(deftype+ Foo [x y]))))
  (is (not= nil (eval '(deftype+ Foo [x y z])))))

(deftest test-defprotocol+
  (is (not= nil (eval '(defprotocol+ Bar (bar [x y])))))
  (is (= nil (eval '(defprotocol+ Bar (bar [x y])))))
  (is (not= nil (eval '(defprotocol+ Bar (bar [x y z]))))))

(deftest test-definterface+
  (is (not= nil (eval '(definterface+ Bar (bar-baz [x y])))))
  (is (= nil (eval '(definterface+ Bar (bar-baz [x y])))))
  (is (not= nil (eval '(definterface+ Bar (bar-baz [x y z]))))))
