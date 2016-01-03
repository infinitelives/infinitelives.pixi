(ns infinitelives.pixi.texture
  (:require [PIXI]))

(defn sub-texture [texture [x y] [w h]]
  (PIXI/Texture. texture (PIXI/Rectangle. x y w h)))

(defonce texture-store (atom {}))

(defn add!
  [key texture]
  (swap! texture-store assoc key texture)
  )

(defn remove!
  ([key]
   (swap! texture-store dissoc key)))

(defn get
  [key]
  (@texture-store key))

(defn load-sprite-sheet!
  [texture asset-description]
  (swap! texture-store
         #(into %
                (for [[key {:keys [pos size]}] asset-description]
                  [key
                   (sub-texture texture pos size)]))))
