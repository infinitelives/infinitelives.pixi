(ns tilemap.core
  (:require  [infinitelives.pixi.canvas :as c]
             [infinitelives.pixi.resources :as r]
             [infinitelives.pixi.tilemap :as tm]
             [infinitelives.utils.events :as e]
             [infinitelives.utils.console :refer [log]]
)

  (:require-macros [cljs.core.async.macros :refer [go]]
                   [infinitelives.pixi.macros :as m]))


(enable-console-print!)

(defonce bg-colour 0x0D0711)

(def tile-map-chars
  [
   "┌──────╖          "
   "│o...oo║          "
   "│.,....║          "
   "│.....o╙─────╖    "
   "│o..,.....,..║    "
   "│o....o╔═══╕.║    "
   "│oo..oo║   │.║    "
   "╘══════╝   │o║    "
   "           │.║    "
   "    ┌──────┘.╙╖   "
   "    │oo..,...o║   "
   "    │..o......║   "
   "    │,.....o..║   "
   "    ╘═════════╝   "])

(def key-for
  {
   "┌" :wall-top-left
   "╖" :wall-top-right
   "╘" :wall-bottom-left
   "╝" :wall-bottom-right
   "─" :wall-top
   "│" :wall-left
   "║" :wall-right
   "═" :wall-bottom
   " " :floor
   "." :floor-2
   "o" :floor-3})

(def tile-set-mapping
  {
   :wall-top-left [48 0]
   :wall-top-right [64 0]
   :wall-bottom-left [48 16]
   :wall-bottom-right [64 16]
   :wall-top [16 32]
   :wall-left [32 16]
   :wall-right [0 16]
   :wall-bottom [16 0]
   :floor [96 0]
   :floor-2 [112 0]
   :floor-3 [128 32]})

(defonce canvas
  (c/init {:layers [:bg :tilemap :ui]
           :background bg-colour
           :expand true}))

(defonce main
  (go
    ;; load resource url with tile sheet
    (<! (r/load-resources canvas :ui ["img/tiles.png"]))

    (let [tile-set (tm/make-tile-set :tiles tile-set-mapping [16 16])]
      (log "tile-set:" tile-set)
      (m/with-sprite :tilemap
        [tile-map (tm/make-tilemap tile-set key-for tile-map-chars
                                   :scale 4)]
        (<! (e/wait-frames 10000))))))
