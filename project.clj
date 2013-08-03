(defproject potemkin "0.3.2"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :description "Some useful facades."
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.2 {:dependencies [[org.clojure/clojure "1.2.1"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0-master-SNAPSHOT"]]}}
  :global-vars {*warn-on-reflection* true}
  :aliases {"all" ["with-profile" "dev,1.2:dev,1.3:dev,1.4:dev:dev,1.6"]}
  :test-selectors {:default #(not (some #{::benchmark}
                                        (cons (:tag %) (keys %))))
                   :benchmark :benchmark
                   :all (constantly true)}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
