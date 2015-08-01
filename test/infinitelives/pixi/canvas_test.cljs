(ns infinitelives.pixi.canvas-test
  (:require [cljs.test :refer-macros [deftest is]]
            [infinitelives.pixi.canvas :as canvas]
            [goog.dom :as dom]))

(defn- nodelist->seq
  "returns a lazy-seq of all the nodes in the nodelist"
  ([nodelist]
   (nodelist->seq nodelist 0))
  ([nodelist i]
   (when (< i (.-length nodelist))
     (cons (aget nodelist i)
           (lazy-seq (nodelist->seq nodelist (inc i)))))))

(defn- remove-all-dom-canvas
  "remove every canvas from the DOM"
  []
  (doseq [node (nodelist->seq (dom/$$ "canvas"))]
    (dom/removeNode node)))

(deftest make
  (is (canvas/make :engine :canvas))
  (is (canvas/make :engine :auto))
  (remove-all-dom-canvas))
