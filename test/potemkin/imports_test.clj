(ns potemkin.imports-test)

(defn multi-arity-fn
  "Here is a doc-string."
  ([x])
  ([x y]))

(defprotocol TestProtocol
  (protocol-function [x a b c] "This is a protocol function."))

(defmacro multi-arity-macro
  "I am described."
  ([a])
  ([a b]))

(defn inlined-fn
  "Faster than the average invocation."
  {:inline (fn [x] x)}
  [x]
  x)

(def some-value 1)

(defn ^clojure.lang.ExceptionInfo ex-info-2 [msg data]
  (ex-info msg data))

(def some-other-value 2)

(defn some-other-fn [] 1)
