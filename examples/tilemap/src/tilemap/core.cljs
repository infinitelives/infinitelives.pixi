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
   " ┌──────╖          "
   " │o...oo║          "
   "<>.,....║   -=     "
   "#$.....o╙───{}╖    "
   " │o..,.....,..║    "
   " │o....o╔═══╕.║    "
   " │oo..oo║   │.║    "
   " ╘()════╝   │o║    "
   "  []   -=   │.║    "
   "     ┌─{}───┘.╙╖   "
   "     │oo..,...o║   "
   "    !@..o......║   "
   "    AB,.....o..║   "
   "     ╘═════════╝   "])

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
   "╔" :wall-top-left-outer
   "╙" :wall-bottom-left-outer
   "╕" :wall-top-right-outer
   "┘" :wall-bottom-right-outer
   "-" :door-top-left
   "=" :door-top-right
   "{" :door-bottom-left
   "}" :door-bottom-right
   "(" :a
   ")" :b
   "[" :c
   "]" :d
   "<" :e
   ">" :f
   "#" :g
   "$" :h
   "!" :i
   "@" :j
   "A" :k
   "B" :l
   "." :floor
   "," :floor-2
   "o" :floor-3
   " " nil})

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
   :wall-top-left-outer [0 0]
   :wall-bottom-left-outer [0 32]
   :wall-top-right-outer [32 0]
   :wall-bottom-right-outer [32 32]
   :floor [96 0]
   :floor-2 [112 0]
   :floor-3 [128 32]
   :door-top-left [32 80]
   :door-top-right [48 80]
   :door-bottom-left [32 96]
   :door-bottom-right [48 96]
   :a [112 112]
   :b [128 112]
   :c [112 128]
   :d [128 128]
   :e [144 144]
   :f [160 144]
   :g [144 160]
   :h [160 160]
   :i [144 176]
   :j [160 176]
   :k [144 192]
   :l [160 192]
})

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
