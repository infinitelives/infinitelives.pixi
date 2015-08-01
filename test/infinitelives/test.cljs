(ns infinitelives.test
  (:require [cljs.test :refer-macros [run-all-tests]]
            [infinitelives.pixi.canvas-test]))

(enable-console-print!)

(defn ^:export run
  []
  (run-all-tests #"infinitelives.*-test"))
