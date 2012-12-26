(defproject potemkin "0.2.0-SNAPSHOT"
  :description "Some useful facades."
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.2 {:dependencies [[org.clojure/clojure "1.2.1"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0-rc1"]]}}
  :aliases {"all" ["with-profile" "1.2,dev:1.3,dev:dev:1.5,dev"]}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
