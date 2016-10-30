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
   "#$.....:╙───{}╖    "
   " │o..,.....,..║    "
   " │o....o╔═══╕.║    "
   " │:o..oo║   │.║    "
   " ╘()════╝   │o║    "
   "  []   -=   │.║    "
   "     ┌─{}───┘.╙╖   "
   "     │oo..,...o║   "
   "    !@..:......║   "
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
   "(" :door-bottom-1
   ")" :door-bottom-2
   "[" :door-bottom-3
   "]" :door-bottom-4
   "<" :door-left-1
   ">" :door-left-2
   "#" :door-left-3
   "$" :door-left-4
   "!" :door-left-shut-1
   "@" :door-left-shut-2
   "A" :door-left-shut-3
   "B" :door-left-shut-4
   "." :floor
   "," :floor-2
   "o" :floor-3
   ":" :floor-4
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
   :floor-4 [128 0]
   :door-top-left [32 80]
   :door-top-right [48 80]
   :door-bottom-left [32 96]
   :door-bottom-right [48 96]
   :door-bottom-1 [112 112]
   :door-bottom-2 [128 112]
   :door-bottom-3 [112 128]
   :door-bottom-4 [128 128]
   :door-left-1 [144 144]
   :door-left-2 [160 144]
   :door-left-3 [144 160]
   :door-left-4 [160 160]
   :door-left-shut-1 [144 176]
   :door-left-shut-2 [160 176]
   :door-left-shut-3 [144 192]
   :door-left-shut-4 [160 192]
})

(defonce canvas
  (c/init {:layers [:bg :tilemap :ui]
           :background bg-colour
           :expand true}))

(defonce main
  (go
    ;; load resource url with tile sheet
    (<! (r/load-resources canvas :ui ["img/tiles.png"]))

    (let [tile-set (tm/make-tile-set :tiles tile-set-mapping [16 16])
          tile-sprites (->> tile-map-chars
                            (tm/make-tile-map key-for)
                            (tm/make-tile-sprites tile-set)
                            )]
      (log "tile-set:" tile-set)
      (log "tile-sprites:" tile-sprites)
      (m/with-sprite :tilemap
        [tile-map (tm/make-tilemap tile-sprites
                                   :scale 4
                                   :particle-opts #{:uvs})]

        (while true
          ;; closed
          (<! (e/wait-frames 120))
          (tm/alter-tile! tile-sprites [0 2] tile-set :door-left-shut-1)
          (tm/alter-tile! tile-sprites [1 2] tile-set :door-left-shut-2)
          (tm/alter-tile! tile-sprites [0 3] tile-set :door-left-shut-3)
          (tm/alter-tile! tile-sprites [1 3] tile-set :door-left-shut-4)

          ;; open
          (<! (e/wait-frames 120))
          (tm/alter-tile! tile-sprites [0 2] tile-set :door-left-1)
          (tm/alter-tile! tile-sprites [1 2] tile-set :door-left-2)
          (tm/alter-tile! tile-sprites [0 3] tile-set :door-left-3)
          (tm/alter-tile! tile-sprites [1 3] tile-set :door-left-4))))))
