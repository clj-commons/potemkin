(defproject potemkin "0.3.10"
  :license {:name "MIT License"}
  :description "Some useful facades."
  :dependencies [[clj-tuple "0.1.5"]
                 [riddley "0.1.7"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]
                                  [criterium "0.4.3"]
                                  [collection-check "0.1.3"]]}}
  :global-vars {*warn-on-reflection* true}
  :aliases {"all" ["with-profile" "dev:dev,1.5"]}
  :test-selectors {:default #(not (some #{:benchmark}
                                        (cons (:tag %) (keys %))))
                   :benchmark :benchmark
                   :all (constantly true)}
  :java-source-paths ["src"]
  :jvm-opts ^:replace ["-server"]
  :javac-options ["-source" "1.6" "-target" "1.6"]
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
