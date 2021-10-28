(defproject daaku/schema "2.0.3"
  :description "Schema validation and transformation, using simple functions."
  :url "https://github.com/daaku/schema"
  :scm {:name "git" :url "https://github.com/daaku/schema"}
  :license {:name "MIT License"}
  :dependencies [[org.clojure/clojure "1.10.3"]]
  :plugins [[lein-doo "0.1.11"]]
  :profiles {:dev {:dependencies [[org.clojure/clojurescript "1.10.879"]]}}
  :cljsbuild {:builds {:test {:source-paths ["src" "test"]
                              :compiler {:target :nodejs
                                         :output-to "target/schema.js"
                                         :main daaku.schema-test}}}}
  :aliases {"test-cljc" ["with-profile" "+test" "do" "test,"
                         "doo" "node" "test" "once"]})
