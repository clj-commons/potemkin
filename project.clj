(defproject potemkin "0.3.3"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :description "Some useful facades."
  :dependencies [[clj-tuple "0.1.2"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.5.1"]
                                  [criterium "0.4.1"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0-master-SNAPSHOT"]]}}
  :global-vars {*warn-on-reflection* true}
  :aliases {"all" ["with-profile" "dev,1.4:dev:dev,1.6"]}
  :test-selectors {:default #(not (some #{:benchmark}
                                        (cons (:tag %) (keys %))))
                   :benchmark :benchmark
                   :all (constantly true)}
  :jvm-opts ^:replace ["-server"]
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
