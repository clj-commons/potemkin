(ns potemkin.collections-test
  (:use
    [clojure test]
    [potemkin]))

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

  (let [kvs (interleave (range 1e5) (map #(- Long/MAX_VALUE %) (range 1e5)))
        h (hash (apply assoc m kvs))]
    ;; is the hashing consistent within the type
    (is (= h (hash (apply assoc m kvs))))
    ;; is it consistent with other maps
    (is (= h (hash (apply hash-map kvs)))))
  
  (let [m (assoc m :a 1)]
    (is (= 1 (count m)))
    (is (contains? m :a))
    (is (= {:a 1} m))
    (is (= 1 (get m :a)))
    (is (= 2 (get m :b 2)))
    (is (= 1 (:a m)))
    (is (= 1 (m :a)))
    (is (= 1 (apply m [:a])))
    (is (= 2 (m :b 2)))
    (is (thrown? Exception (m :b 2 3)))
    (is (= [:a] (keys m)))
    (is (= [[:a 1]] (seq m))))

  (let [m (assoc m :a 1)]
    (is (= {:a 1, :b 2, :c 3}
          (merge m {:b 2} {:c 3})
          (merge m {:b 2, :c 3})
          (merge {:b 2, :c 3} m))))

  (let [m (-> m (assoc :a 1) (dissoc :a))]
    (is (not (contains? m :a))))

  (let [s (-> m (assoc :a 1) seq)]
    (is (= [[:a 1]] s)))

  (let [m (conj m [:a 1])]
    (is (= {:a 1} m)))

  (let [m (-> m (assoc :a 1) (assoc :b 2))
        s (reduce
           (fn [s [k v]]
             (+ s v))
           0
           m)]
    (is (= 3 s))))

(deftest test-maps
  (test-basic-map-functionality (SimpleMap. {}))
  (test-basic-map-functionality (->SimpleDerivedMap)))

(deftest test-derived-map
  (let [m (->DerivedMap "AbC")]
    (is (= {:string "AbC" :lower "abc" :upper "ABC"} m))
    (is (= {:lower "abc" :upper "ABC"} (dissoc m :string)))
    (is (= {:string "foo" :lower "abc" :upper "ABC" :bar "baz"}
          (assoc m :string "foo" :bar "baz")))))
