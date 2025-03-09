(ns potemkin.namespaces-test
  (:require
   [clojure.repl :as repl :refer :all]
   [clojure.string :as str]
   [clojure.test :refer :all]
   [potemkin :refer :all]
   [potemkin.imports-test :as i]))

(import-macro i/multi-arity-macro)
(import-macro i/multi-arity-macro alt-macro-name)
(import-fn i/multi-arity-fn)
(import-fn i/multi-arity-fn alt-name)
(import-fn i/protocol-function)
(import-fn i/inlined-fn)
(import-def i/some-value)
(import-vars [i :refer [some-other-fn]])
(import-vars [i
              :refer [multi-arity-macro multi-arity-fn some-value some-other-value]
              :rename {multi-arity-macro renamed-multi-arity-macro
                       multi-arity-fn renamed-multi-arity-fn
                       some-value renamed-some-value}])

(defn drop-lines [n s]
  (->> s str/split-lines (drop n) (interpose "\n") (apply str)))

(defmacro out= [& args]
  `(= ~@(map (fn [x] `(with-out-str ~x)) args)))

(defmacro rest-out= [& args]
  `(= ~@(map (fn [x] `(drop-lines 2 (with-out-str ~x))) args)))

(deftest test-import-macro
  (is (out= (source i/multi-arity-macro) (source multi-arity-macro)))
  (is (rest-out= (doc i/multi-arity-macro) (doc multi-arity-macro)))
  (is (out= (source i/multi-arity-macro) (source alt-macro-name)))
  (is (rest-out= (doc i/multi-arity-macro) (doc alt-macro-name))))

(deftest test-import-fn
  (is (= 1 (inlined-fn 1)))
  (is (= 1 (apply inlined-fn [1])))
  (is (out= (source i/multi-arity-fn) (source multi-arity-fn)))
  (is (rest-out= (doc i/multi-arity-fn) (doc multi-arity-fn)))
  (is (out= (source i/multi-arity-fn) (source alt-name)))
  (is (rest-out= (doc i/multi-arity-fn) (doc alt-name)))
  (is (rest-out= (doc i/protocol-function) (doc protocol-function))))

(deftest test-points-to-the-value-after-reload
  (is (= 1 some-value))
  (require 'potemkin.imports-test :reload)
  (is (= 1 some-value)))

(deftest import-vars-throws-if-missing-var
  (try
    (import-vars [clojure.set union onion-misspelled])
    (is false "`import-vars` should have thrown an exception")
    (catch Exception ex
      (is "`clojure.set/onion-misspelled` does not exist" (.getMessage ex)))))

(deftest import-vars-supports-refers
  (is (fn? some-other-fn)))

(deftest import-vars-allows-renaming
  (is (= 1 renamed-some-value))
  (is (= 2 some-other-value))
  (is (out= (source i/multi-arity-fn) (source renamed-multi-arity-fn)))
  (is (rest-out= (doc i/multi-arity-fn) (doc renamed-multi-arity-fn)))
  (is (out= (source i/multi-arity-macro) (source renamed-multi-arity-macro)))
  (is (rest-out= (doc i/multi-arity-macro) (doc renamed-multi-arity-macro))))

;; This is the whole test for CLJ-1929
(import-vars [potemkin.imports-test ex-info-2])
