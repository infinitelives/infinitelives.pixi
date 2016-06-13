(ns infinitelives.pixi.sprite
  (:require [cljsjs.pixi]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.pixi.texture :as texture]
            )
)

(defn make-point
  "Make a PIXI Point from x and y"
  [x y]
  (js/PIXI.Point. x y))

(defn make-sprite
  "construct a sprite by its texture. optionally pass in other things"
  [texture & {:keys [x y xhandle yhandle scale alpha interactive mousedown
                     rotation tint tiling tiling-width tiling-height visible]
              :or {x 0 y 0
                   xhandle 0.5 yhandle 0.5
                   scale 1
                   alpha 1
                   rotation 0
                   tiling false}}]
  (let [s (if tiling
            (js/PIXI.extras.TilingSprite.
             (if (keyword? texture) (texture/get texture) texture)
             tiling-width tiling-height)
            (js/PIXI.Sprite. (if (keyword? texture) (texture/get texture) texture)))]
    (assert s "creation of sprite failed and returned nil")
    (set! (.-anchor s) (make-point xhandle yhandle))
    (set! (.-x s) x)
    (set! (.-y s) y)
    (set! (.-rotation s) rotation)
    (set! (.-visibile s) visible)
    (when-not (= scale 1)
      (set! (.-scale s)
            (if (number? scale)
              (make-point scale scale)
              (make-point (get scale 0) (get scale 1)))))
    (when-not (= 1 alpha)
      (set! (.-alpha s) alpha))
    (when-not (nil? interactive) (set! (.-interactive s) interactive))
    (when-not (nil? mousedown) (set! (.-mousedown s) mousedown))
    (when-not (nil? tint) (set! (.-tint s) tint))
    s))

(defn set-pos!
  ([sprite x y]
   (set! (.-position.x sprite) x)
   (set! (.-position.y sprite) y))
  ([sprite pos]
   (if (vector? pos)
     (set-pos! sprite (pos 0) (pos 1))
     (set-pos! sprite (aget pos 0) (aget pos 1)))))

(defn set-x! [sprite x]
  (set! (.-x sprite) x))

(defn set-y! [sprite y]
  (set! (.-y sprite) y))

(defn set-anchor! [sprite x y]
  (set! (.-anchor sprite) (make-point x y)))

(defn set-alpha! [sprite alpha]
  (set! (.-alpha sprite) alpha))

(defn set-pivot! [sprite x y]
  (set! (.-pivot.x sprite) x)
  (set! (.-pivot.y sprite) y))

(defn set-scale! ([sprite s]
                  (set! (.-scale sprite) (make-point s s)))
  ([sprite sx sy]
   (set! (.-scale sprite) (make-point sx sy))))

(defn set-rotation! [sprite theta]
  (set! (.-rotation sprite) theta))

(defn set-texture! [sprite tex]
  (if (keyword? tex)
    (set! (.-texture sprite) (texture/get tex))
    (set! (.-texture sprite) tex)))

(defn set-visible! [sprite visibility]
  (set! (.-visible sprite) visibility))

(defn get-pos
  "return the position of sprite as a vec2.
  optionally pass in an x and y offset to add
  to the returned vector
  "
  [sprite & [offset-x offset-y]]
  (vec2/vec2 (+ (or offset-x 0) (.-position.x sprite))
             (+ (or offset-y 0) (.-position.y sprite))))

(defn get-x [sprite]
  (.-position.x sprite))

(defn get-y [sprite]
  (.-position.y sprite))

(defn get-xy [sprite]
  [(.-position.x sprite)
   (.-position.y sprite)])
