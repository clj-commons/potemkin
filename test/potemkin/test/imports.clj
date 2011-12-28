;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin.test.imports)

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
