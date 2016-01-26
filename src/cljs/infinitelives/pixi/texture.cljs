(ns infinitelives.pixi.texture
  (:require [cljsjs.pixi]))

(defn sub-texture [texture [x y] [w h]]
  (js/PIXI.Texture. texture (js/PIXI.Rectangle. x y w h)))

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

(defn set-texture!
  [key texture]
  (swap! texture-store assoc key texture))

(defn load-sprite-sheet!
  [texture asset-description]
  (swap! texture-store
         #(into %
                (for [[key {:keys [pos size]}] asset-description]
                  [key
                   (sub-texture texture pos size)]))))
