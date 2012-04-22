;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin.namespace
  (:use [clojure pprint]))

(defmacro import-fn 
  "Given a function in another namespace, defines a function with the same name in the
   current namespace.  Argument lists, doc-strings, and original line-numbers are preserved."
  [sym]
  (let [vr (resolve sym)
        m (meta vr)
        nspace (:name m)
        n (:name m)
        arglists (:arglists m)
        doc (:doc m)
        protocol (:protocol m)]
    (when-not vr
      (throw (IllegalArgumentException. (str "Don't recognize " sym))))
    (when (:macro m)
      (throw (IllegalArgumentException. (str "Calling import-fn on a macro: " sym))))
    `(do
       (def ~(with-meta n {:protocol protocol}) (deref ~vr))
       (alter-meta! (var ~n) assoc
         :doc ~doc
         :arglists ~(list 'quote arglists)
         :file ~(:file m)
         :line ~(:line m))
       ~vr)))

(defmacro import-macro
  "Given a macro in another namespace, defines a macro with the same name in the
   current namespace.  Argument lists, doc-strings, and original line-numbers are preserved."
  [sym]
  (let [vr (resolve sym)
        m (meta vr)
        n (:name m)
        nspace (:ns m)
        arglists (:arglists m)
        doc (:doc m)]
    (when-not vr
      (throw (IllegalArgumentException. (str "Don't recognize " sym))))
    (when-not (:macro m)
      (throw (IllegalArgumentException. (str "Calling import-macro on a non-macro: " sym))))
    `(do
       (def ~n ~(resolve sym))
       (alter-meta! (var ~n) assoc
         :doc ~doc
         :arglists ~(list 'quote arglists)
         :file ~(:file m)
         :line ~(:line m))
       (.setMacro (var ~n))
       ~vr)))
