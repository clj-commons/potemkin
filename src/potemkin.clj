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

   unify-gensyms
   normalize-gensyms
   equivalent?]

  [potemkin.utils

   condp-case
   try*
   fast-bound-fn
   fast-bound-fn*
   fast-memoize
   doit
   doary]

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
