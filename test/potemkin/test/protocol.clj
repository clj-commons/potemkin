(ns potemkin.test.protocol)

(defn multi-arity
  "Here is a doc-string."
  ([x])
  ([x y]))

(defprotocol TestProtocol
  (protocol-function [x a b c] "This is a protocol function."))
