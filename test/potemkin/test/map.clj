;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin.test.map
  (:use
    [clojure test]
    [potemkin map]))

(def-custom-map SimpleMap)

(def-custom-map SimpleOverrides
  :get (fn [_ data _ key default-value]
         `(get ~data ~key ~default-value))
  :assoc (fn [type-name data _ key value]
           `(new ~type-name (assoc ~data ~key ~value)))
  :dissoc (fn [type-name data _ key]
            `(new ~type-name (dissoc ~data ~key)))
  :keys (fn [_ data _]
          `(keys ~data)))

(defn test-basic-map-functionality [m]
  (let [m (assoc m :a 1)]
    (is (= 1 (count m)))
    (is (contains? m :a))
    (is (= {:a 1} m))
    (is (= 1 (get m :a)))
    (is (= 2 (get m :b 2)))
    (is (= 1 (:a m)))
    (is (= 1 (m :a))))
  (let [m (-> m (assoc :a 1) (dissoc :a))]
    (is (not (contains? m :a))))
  (let [s (-> m (assoc :a 1) seq)]
    (is (= [[:a 1]] s)))
  (let [m (conj m [:a 1])]
    (is (= {:a 1} m))))

(deftest test-maps
  (test-basic-map-functionality (SimpleMap. {}))
  (test-basic-map-functionality (SimpleOverrides. {})))


