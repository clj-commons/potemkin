;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin
  (:require
    [potemkin namespaces types collections macros utils]))

(potemkin.namespaces/import-vars potemkin.namespaces/import-vars) ;; totally meta

(import-vars
  [potemkin.namespaces

   import-fn
   import-macro
   import-def]

  [potemkin.macros

   macroexpand+
   unify-gensyms
   normalize-gensyms
   equivalent?]

  [potemkin.utils
   
   condp-case
   try*
   fast-bound-fn
   fast-bound-fn*
   fast-memoize]

  [potemkin.types

   def-abstract-type
   reify+
   defprotocol+
   deftype+
   defrecord+
   definterface+
   extend-protocol+]

  [potemkin.collections

   reify-map-type
   def-derived-map
   def-map-type])





