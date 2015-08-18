(ns infinitelives.pixi.texture
  (:require [PIXI]))

(defn sub-texture [texture [x y] [w h]]
  (PIXI/Texture. texture (PIXI/Rectangle. x y w h)))
