(defproject griffinbank/potemkin "0.4.9-20250811"
  :license {:name "MIT License"}
  :description "Some useful facades."
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :sign-releases false}]]

  :dependencies [[riddley "0.1.12"]]
  :profiles {:dev {:dependencies [[criterium "0.4.6"]
                                  [collection-check "0.1.6"]]}
             :provided {:dependencies [[org.clojure/clojure "1.11.1"]]}}
  :global-vars {*warn-on-reflection* true}
  :test-selectors {:default #(not (some #{:benchmark}
                                        (cons (:tag %) (keys %))))
                   :benchmark :benchmark
                   :all (constantly true)}
  :java-source-paths ["src"]
  :javac-options ["-source" "1.8" "-target" "1.8"]
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"
                 "griffin.maven-banksy" {:url "https://griffin-852583658123.d.codeartifact.us-east-1.amazonaws.com/maven/maven-banksy/"
                                         :username "aws"
                                         :password :env/CODEARTIFACT_AUTH_TOKEN}})
