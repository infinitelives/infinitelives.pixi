(defproject infinitelives.pixi "0.1.0-SNAPSHOT"
  :description "2D pixi library for cljs games"
  :url "https://github.com/infinitelives"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 ;; should we depend on this? maybe its better without?
                 [prismatic/dommy "1.1.0"]
                 ]

  :plugins [[lein-cljsbuild "1.0.6"]]

  :source-paths ["src/cljs"]
  :test-paths ["test"]

  :cljsbuild
  {
   :builds {:test
            {:source-paths ["src" "test"]
             :compiler {:output-to "resources/test/compiled.js"
                        :optimizations :whitespace
                        :pretty-print true}}}
   :test-commands {"test" ["phantomjs"
                           "resources/test/test.js"
                           "resources/test/test.html"]}})
