(defproject potemkin "0.2.2"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :description "Some useful facades."
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.2 {:dependencies [[org.clojure/clojure "1.2.1"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0-master-SNAPSHOT"]]}}
  :aliases {"all" ["with-profile" "1.2,dev:1.3,dev:dev:1.5,dev"]}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})