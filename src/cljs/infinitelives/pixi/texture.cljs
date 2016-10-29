(ns infinitelives.pixi.texture
  (:require [cljsjs.pixi]))

(defonce !texture-store (atom {}))

(defn add!
  "Add a texture object to the texture cache under
  the key `key`"
  [key texture]
  (swap! !texture-store assoc key texture))

(defn remove!
  "Remove the texture keyed as `key` from the
  texture cache"
  [key]
  (swap! !texture-store dissoc key))

(defn empty!
  "Empty the texture cache of all its textures"
  []
  (reset! !texture-store {}))

(defn get-texture
  "Get a texture from the cache by `key`"
  [key]
  (or
   (@!texture-store key)
   (throw (js/Error. (str "Texture " key " not loaded")))))

(defn sub-texture [texture [x y] [w h]]
  (js/PIXI.Texture. texture (js/PIXI.Rectangle. x y w h)))

(defn load-sprite-sheet!
  "Given a texture and a sprite sheet discription
  dictionary, load all the subtextures into the cache
  keyed by the keywords defined in `asset-description`.

  The asset description value looks like:

  {
     :tex1 {:pos [x1 y1] :size [w1 h1]}
     :tex2 {:pos [x2 y2] :size [w2 h2]}
     ...
  }
  "
  [texture asset-description]
  (swap! !texture-store into (for [[key {:keys [pos size]}] asset-description]
                              [key (sub-texture texture pos size)])))
