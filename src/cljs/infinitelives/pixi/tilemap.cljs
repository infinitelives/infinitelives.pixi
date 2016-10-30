(ns infinitelives.pixi.tilemap
  (:require [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.sprite :as s]
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
                         tile-map]
  (into
   {}
   (filter identity
           (for [row (range (count tile-map))
                 col (range (count (first tile-map)))]
             (let [char (nth (tile-map row) col)]
               (when char [[col row]
                           (s/make-sprite (texture-map char)
                                          :x (* tile-width col) :y (* tile-height row)
                                          :xhandle 0 :yhandle 0)]))))))

(defn make-tile-map [tile-mapping tile-map-chars]
  (strs->keymap tile-mapping tile-map-chars))

(defn make-tilemap [sprite-set & opts]
  (apply s/make-container
         :children (vals sprite-set)
         :xhandle 0.5 :yhandle 0.5
         :particle true
         opts))

(defn alter-tile! [sprite-set coord tile-set texture-key]
  (set!
   (.-texture (sprite-set coord))
   (-> tile-set :texture-map texture-key)))
