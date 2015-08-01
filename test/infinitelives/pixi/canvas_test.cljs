(ns infinitelives.pixi.canvas-test
  (:require [cljs.test :refer-macros [deftest is]]
            [infinitelives.pixi.canvas :as canvas]))

(deftest make
  (let [c (canvas/make)]
    (is c)))
