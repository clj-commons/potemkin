(ns potemkin.collections-test
  (:use
    [clojure test]
    [potemkin])
  (:require
    [collection-check :as check]
    [simple-check.generators :as gen]))

(def-map-type SimpleMap [m mta]
  (get [_ k d] (get m k d))
  (assoc [_ k v] (SimpleMap. (assoc m k v) mta))
  (dissoc [_ k] (SimpleMap. (dissoc m k) mta))
  (keys [_] (keys m))
  (with-meta [_ mta] (SimpleMap. m mta))
  (meta [_] mta))

(def-derived-map SimpleDerivedMap [])

(def-derived-map DerivedMap [^String s]
  :string s
  :lower (.toLowerCase s)
  :upper (.toUpperCase s))

(defn test-basic-map-functionality [m]
  (check/assert-map-like m gen/keyword gen/pos-int))

(deftest test-maps
  (test-basic-map-functionality (->SimpleMap {} {}))
  (let [m (->SimpleMap {} {})] 
    (is (= (::meta-key (meta (with-meta m {::meta-key "value"})))
           "value")))
  (test-basic-map-functionality (->SimpleDerivedMap)))

(deftest test-derived-map
  (let [m (->DerivedMap "AbC")]
    (is (= {:string "AbC" :lower "abc" :upper "ABC"} m))
    (is (= {:lower "abc" :upper "ABC"} (dissoc m :string)))
    (is (= {:string "foo" :lower "abc" :upper "ABC" :bar "baz"}
          (assoc m :string "foo" :bar "baz")))))
