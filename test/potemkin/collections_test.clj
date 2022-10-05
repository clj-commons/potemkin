(ns potemkin.collections-test
  (:require
    [collection-check :as check]
    [clojure.test :refer :all]
    [clojure.test.check.generators :as gen]
    [potemkin.collections :refer :all]))

(def-map-type SimpleMap [m mta]
  (get [_ k d] (get m k d))
  (assoc [_ k v] (SimpleMap. (assoc m k v) mta))
  (dissoc [_ k] (SimpleMap. (dissoc m k) mta))
  (keys [_] (keys m))
  (with-meta [_ mta] (SimpleMap. m mta))
  (meta [_] mta))

(defn simple-map [m mta]
  (reify-map-type
    (get [_ k d] (get m k d))
    (assoc [_ k v] (simple-map (assoc m k v) mta))
    (dissoc [_ k] (simple-map (dissoc m k) mta))
    (keys [_] (keys m))))

(deftest test-simple-map-equiv
  (is (= (java.util.HashMap.) (simple-map {} {}))))

(def-derived-map SimpleDerivedMap [])

(def-derived-map DerivedMap [^String s]
  :string s
  :lower (.toLowerCase s)
  :upper (.toUpperCase s))

(defn test-basic-map-functionality [m]
  (check/assert-map-like 1e3 m gen/pos-int gen/pos-int))

(deftest test-maps
  (test-basic-map-functionality (->SimpleMap {} {}))
  (let [m (->SimpleMap {} {})]
    (is (= (::meta-key (meta (with-meta m {::meta-key "value"})))
           "value")))
  (test-basic-map-functionality (->SimpleDerivedMap))
  (test-basic-map-functionality (simple-map {} {}))
  (is (= [:one "two"] (find (->SimpleMap {:one "two" :three "four"} {}) :one)))
  (is (= {:old 1 :new 2} (conj (->SimpleMap {:old 1} {}) {:new 2})))
  (is (= {:old 1 :new 2} (conj (->SimpleMap {:old 1} {}) (doto (java.util.HashMap.)
                                                           (.put :new 2))))))

(defn test-derived-map [f]
  (let [m (f "AbC")]
    (is (= {:string "AbC" :lower "abc" :upper "ABC"} m))
    (is (= {:lower "abc" :upper "ABC"} (dissoc m :string)))
    (is (= {:string "foo" :lower "abc" :upper "ABC" :bar "baz"}
          (assoc m :string "foo" :bar "baz")))
    (is (= #{:lower :upper :string} (-> m keys set)))
    (is (= [:lower "abc"] (find m :lower)))
    (is (= {:lower "abc" :upper "ABC"} (select-keys m [:lower :upper])))))

(deftest test-derived-maps
  (test-derived-map ->DerivedMap))

(def-map-type LazyMap [m]
  (get [_ k default-value]
       (if (contains? m k)
         (let [v (get m k)]
           (if (instance? clojure.lang.Delay v)
             @v
             v))
         default-value))
  (assoc [_ k v]
    (LazyMap. (assoc m k v)))
  (dissoc [_ k]
          (LazyMap. (dissoc m k)))
  (keys [_]
        (keys m)))

(deftest map-entries-are-lazy
  (let [was-called (atom false)
        d (delay (reset! was-called true))
        m (LazyMap. {:d d})]

    (doall (keys m))
    (is (= false @was-called))))
