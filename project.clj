(defproject infinitelives/infinitelives.pixi "0.1.2-SNAPSHOT"
  :description "2D pixi library for rapidly developing games in clojurescript"
  :url "https://github.com/infinitelives/infinitelives.pixi"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.521"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 ;; depend on utils
                 [infinitelives/infinitelives.utils "0.1.1-SNAPSHOT"]

                 ;; we need pixi
                 [cljsjs/pixi "4.5.3-0"]

                 ;; should we depend on this? maybe its better without?
                 [prismatic/dommy "1.1.0"]
                 ]

  :plugins [[lein-cljsbuild "1.0.6"]]

  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test"]

  :cljsbuild
  {
   :builds {:test
            {:source-paths ["src" "test"]
             :compiler {:output-to "resources/test/compiled.js"
                        :optimizations :advanced
                        :pretty-print false

                        ;; pixi uses techniques incompatible with
                        ;; google closure optimisation, so we preserve
                        ;; its namespace
                        :externs ["src/js/pixi-externs.js"]
                        :foreign-libs
                        [{:file "https://raw.githubusercontent.com/pixijs/pixi.js/v2.2.9/bin/pixi.js"
                          :provides ["PIXI"]}]
                        }}}
   :test-commands {"test" ["phantomjs"
                           "resources/test/test.js"
                           "resources/test/test.html"]}})
