;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns potemkin
  (:require
    [potemkin.namespace :as namespace]
    [potemkin.map :as map]
    [potemkin.macros :as macros]
    [potemkin.protocols :as protocols]))

(namespace/import-macro namespace/import-macro) ;; totally meta
(import-macro namespace/import-fn)

(import-macro map/def-custom-map)

(import-fn macros/unify-gensyms)
(import-fn macros/transform-defn-bodies)
(import-fn macros/transform-fn-bodies)

(import-macro protocols/defprotocol-once)
(import-macro protocols/deftype-once)
(import-macro protocols/defrecord-once)



