(ns infinitelives.test
  (:require [cljs.test :refer-macros [run-all-tests]]
            [infinitelives.pixi.canvas-test]
            [infinitelives.pixi.resources-test]
            [infinitelives.pixi.texture-test]))

(enable-console-print!)

(defn ^:export run
  []
  (run-all-tests #"infinitelives.*-test"))
