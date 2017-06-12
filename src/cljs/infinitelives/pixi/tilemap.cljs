(ns infinitelives.pixi.tilemap
  (:require [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.console :refer [log]]
)
  )

(defn strs->keymap [key-for strs]
  (mapv #(mapv key-for %) strs))

(defrecord Tileset [texture-map tile-size])

(defn make-tile-set [resource-key mapping tile-size]
  (->Tileset (let [texture (r/get-texture resource-key :nearest)]
               (->> mapping
                    (map (fn [[c pos]]
                           [c (t/sub-texture texture pos tile-size)]))
                    (into {})))
             tile-size))

(defn make-tile-sprites [{texture-map :texture-map
                          [tile-width tile-height] :tile-size}
                         tile-map
                         & {:keys [xoffset yoffset] :or {xoffset 0 yoffset 0}}
                         ]
  (let [tmarray (to-array-2d tile-map)]
    (into
     {}
     (filter identity
             (for [row (range (count tile-map))
                   col (range (count (first tile-map)))]
               (let [char
                     (aget tmarray row col)
                     ;(nth (tile-map row) col)
                     ]
                 (when char [[(+ xoffset col) (+ yoffset row)]
                             (s/make-sprite (texture-map char)
                                            :x (* tile-width (+ xoffset col)) :y (* tile-height (+ yoffset row))
                                            :xhandle 0 :yhandle 0)])))))))

(defn make-tile-map [tile-mapping tile-map-chars & {:keys [xoffset yoffset] :or {xoffset 0 yoffset 0}}]
  (let [orig
        (strs->keymap tile-mapping tile-map-chars)
        height (count orig)
        width (count (first orig))
        empty-line (into [] (repeat width nil))
        padded (concat (repeat yoffset empty-line) orig)
        padded2 (for [line padded] (concat (repeat xoffset nil) line))
        ]
    (mapv vec padded2)))

(defn make-tilemap [sprite-set & opts]
  (apply s/make-container
         :children (vals sprite-set)
         :xhandle 0.5 :yhandle 0.5
         :particle true
         opts))


(defn alter-tile! [layer sprite-set coord tile-set texture-key]
  (assert (sprite-set coord) (str "Trying to alter position " coord " where no tile is placed is forbidden"))
  (set!
   (.-texture (sprite-set coord))
   (-> tile-set :texture-map texture-key))
  sprite-set)

(defn add-tile! [layer sprite-set coord tile-set texture-key]
  (assert (not (sprite-set coord)) (str "Can only add a new tile to an empty position. Coord " coord " already has tile " (sprite-set coord)))
  (let [tile-width 16
        tile-height 16
        [col row] coord
        new-sprite (s/make-sprite (-> tile-set :texture-map texture-key)
                                  :x (* tile-width col) :y (* tile-height row)
                                  :xhandle 0 :yhandle 0)]
    (.addChild layer new-sprite)
    (assoc sprite-set coord new-sprite)))

(defn change-tile! [layer sprite-set coord tile-set texture-key]
  (if (sprite-set coord)
    (alter-tile! layer sprite-set coord tile-set texture-key)
    (add-tile! layer sprite-set coord tile-set texture-key)
)
)

(defn tile-pos->pixel-pos [{[w h] :tile-size} pos]
  (let [x (vec2/get-x pos)
        y (vec2/get-y pos)
        ]
    (vec2/vec2 (* w (+ 0.5 x))
               (* h (+ 0.5 y)))))

(defn pixel-pos->tile-pos [{[w h] :tile-size} pos]
  (let [x (vec2/get-x pos)
        y (vec2/get-y pos)
        ]
    (vec2/vec2 (/ (+ -0.5 x) w)
               (/ (+ -0.5 y) h))))

(defn pixel-pos->tile-pos2 [{[w h] :tile-size} pos]
  (let [x (vec2/get-x pos)
        y (vec2/get-y pos)
        ]
    (vec2/vec2 (/ x w)
               (/ y h))))

(defn tile-pos->pixel-pos2 [{[w h] :tile-size} pos]
  (let [x (vec2/get-x pos)
        y (vec2/get-y pos)
        ]
    (vec2/vec2 (* w x)
               (* h y))))
