;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin.namespace)

(defmacro import-fn 
  "Given a function in another namespace, defines a function with the same name in the
   current namespace.  Argument lists, doc-strings, and original line-numbers are preserved."
  [sym]
  (let [m (meta (eval sym))
        m (meta (intern (:ns m) (:name m)))
        n (:name m)
        arglists (:arglists m)
        doc (:doc m)]
    #_`(do
       (def ~n ~(ns-resolve (:ns m) (:name m)))
       (alter-meta! ~(list 'var n) assoc
         :doc ~doc
         :arglists ~(list 'quote arglists)
         :file ~(:file m)
         :line ~(:line m))
       ~(list 'var n))
     (list `def (with-meta n {:doc doc :arglists (list 'quote arglists) :file (:file m) :line (:line m)}) (eval sym))))

(defmacro import-macro
  "Given a macro in another namespace, defines a macro with the same name in the
   current namespace.  Argument lists, doc-strings, and original line-numbers are preserved."
  [sym]
  (let [sym (eval sym)
        m (meta sym)
        m (meta (intern (:ns m) (:name m)))
        n (:name m)
        arglists (:arglists m)
        doc (:doc m)
        args-sym (gensym "args")]
    `(do
       (defmacro ~(with-meta n {:arglists arglists})
         [~'& ~args-sym]
         (list* ~sym ~args-sym))
       (alter-meta! ~(list 'var n) assoc
         :doc ~doc
         :file ~(:file m)
         :line ~(:line m))
       ~(list 'var n))))
