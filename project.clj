(defproject daaku/schema "0.1.3"
  :description "Schema validation and transformation, using simple functions."
  :url "https://github.com/daaku/schema"
  :scm {:name "git" :url "https://github.com/daaku/schema"}
  :license {:name "MIT License"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :plugins [[lein-doo "0.1.11"]]
  :profiles {:dev {:dependencies [[org.clojure/clojurescript "1.10.773"]]}}
  :cljsbuild {:builds {:test {:source-paths ["src" "test"]
                              :compiler {:target :nodejs
                                         :output-to "target/schema.js"
                                         :main schema-test}}}}
  :aliases {"test-cljc" ["with-profile" "+test" "do" "test,"
                         "doo" "node" "test" "once"]})
