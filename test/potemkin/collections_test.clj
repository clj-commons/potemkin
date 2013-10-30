(ns potemkin.collections-test
  (:use
    [clojure test]
    [potemkin])
  (:require
    [collection-check :as check]
    [simple-check.generators :as gen]))

(def-map-type SimpleMap [m]
  (get [_ k d] (get m k d))
  (assoc [_ k v] (SimpleMap. (assoc m k v)))
  (dissoc [_ k] (SimpleMap. (dissoc m k)))
  (keys [_] (keys m)))

(def-derived-map SimpleDerivedMap [])

(def-derived-map DerivedMap [^String s]
  :string s
  :lower (.toLowerCase s)
  :upper (.toUpperCase s))

(defn test-basic-map-functionality [m]
  (check/assert-map-like 1e4 m gen/keyword gen/pos-int))

(deftest test-maps
  (test-basic-map-functionality (SimpleMap. {}))
  (test-basic-map-functionality (->SimpleDerivedMap)))

(deftest test-derived-map
  (let [m (->DerivedMap "AbC")]
    (is (= {:string "AbC" :lower "abc" :upper "ABC"} m))
    (is (= {:lower "abc" :upper "ABC"} (dissoc m :string)))
    (is (= {:string "foo" :lower "abc" :upper "ABC" :bar "baz"}
          (assoc m :string "foo" :bar "baz")))))
