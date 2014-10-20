(defproject potemkin "0.3.11"
  :license {:name "MIT License"}
  :description "Some useful facades."
  :dependencies [[clj-tuple "0.1.7"]
                 [riddley "0.1.7"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0-alpha2"]
                                  [criterium "0.4.3"]
                                  [collection-check "0.1.3"]]}}
  :global-vars {*warn-on-reflection* true}
  :test-selectors {:default #(not (some #{:benchmark}
                                        (cons (:tag %) (keys %))))
                   :benchmark :benchmark
                   :all (constantly true)}
  :java-source-paths ["src"]
  :jvm-opts ^:replace ["-server"]
  :javac-options ["-source" "1.6" "-target" "1.6"]
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
