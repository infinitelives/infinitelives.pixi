(ns basic.core
    (:require [infinitelives.pixi.canvas :as c]
              [infinitelives.pixi.events :as e]
              [infinitelives.pixi.resources :as r]
              [infinitelives.pixi.texture :as t]
              [infinitelives.pixi.sprite :as s]
              [cljs.core.async :refer [<!]])
    (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce canvas
  (c/init {:layers [:bg] :background 0x1099bb :engine :canvas}))

(defonce main-thread
  (go
    (<! (r/load-resources canvas :bg ["img/bunny.png"]))

    (t/add! :rabbit (r/get-texture :bunny :nearest))

    (c/with-sprite canvas :bg
      [rabbit (s/make-sprite :rabbit)]
      (loop [angle 0]
        (s/set-rotation! rabbit angle)
        (<! (e/next-frame))
        (recur (+ 0.1 angle))))))
