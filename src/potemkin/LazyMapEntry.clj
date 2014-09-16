(ns potemkin.LazyMapEntry
  (:import (clojure.lang ILookup MapEntry))
  (:gen-class :extends clojure.lang.MapEntry
              :init init
              :constructors {[clojure.lang.ILookup Object] [Object Object]}
              :state state))

(defn -init [map k]
  [[k nil] map])

(defn -val [^potemkin.LazyMapEntry this]
  (.valAt ^ILookup (.state this) (.key this)))
