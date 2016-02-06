(ns pixelfont.core
  (:require [infinitelives.pixi.canvas :as c]
            [infinitelives.pixi.events :as e]
            [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.pixelfont :as pf]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [infinitelives.pixi.macros :as m]
                   [infinitelives.pixi.pixelfont :as pf]))

(defonce canvas
  (c/init {:layers [:bg]
           :background 0x404070
           :expand true}))

(defonce main-thread
  (go
    (<! (r/load-resources canvas :bg ["img/fonts.png"]))

    (pf/pixel-font :big "img/fonts.png" [127 84] [500 128]
                   :chars ["ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                           "abcdefghijklmnopqrstuvwxyz"
                           "0123456789!?#`'.,"]
                   :kerning {"fo" -2  "ro" -1 "la" -1 })


    (m/with-sprite canvas :bg
      [text (pf/make-text :big "The quick brown fox jumped over the lazy sequence!"
                          :tint 0xb0c0ff
                          :scale 3
                          :rotation 0
                          )]
      (loop [f 0]
        (s/set-rotation! text (* 0.002 f))
        (s/set-scale! text (+ 2 (* 2 (Math/abs (Math/sin (* f 0.02))))))
        (<! (e/next-frame))
        (recur (inc f))))))
