(ns infinitelives.pixi.resources-test
  (:require [cljs.test :refer-macros [deftest is]]
            [infinitelives.pixi.resources :as resources]))

(comment (deftest make
           (is (canvas/make :engine :canvas))
           (is (canvas/make :engine :auto))
           (remove-all-dom-canvas)))
