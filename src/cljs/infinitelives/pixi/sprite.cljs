(ns infinitelives.pixi.sprite
  (:require [PIXI]
            )
)

(defn make-point
  "Make a PIXI Point from x and y"
  [x y]
  (PIXI/Point. x y))

(defn make-sprite
  "construct a sprite by its texture. optionally pass in other things"
  [texture & {:keys [x y xhandle yhandle scale alpha]
              :or {x 0 y 0
                   xhandle 0.5 yhandle 0.5
                   scale [1 1]
                   alpha 1}}]
  (let [s (PIXI/Sprite. texture)]
    (set! (.-anchor s) (make-point xhandle yhandle))
    (set! (.-x s) x)
    (set! (.-y s) y)
    (when-not (= scale [1 1])
      (set! (.-scale s) (make-point (get scale 0) (get scale 1))))
    (when-not (= 1 alpha)
      (set! (.-alpha s) alpha))
    s))

(defn set-pos!
  ([sprite x y]
   (set! (.-position.x sprite) x)
   (set! (.-position.y sprite) y))
  ([sprite pos]
   (if (vector? pos)
     (set-pos! sprite (pos 0) (pos 1))
     (set-pos! sprite (aget pos 0) (aget pos 1))
     )))

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

(defn set-texture! [sprite tex]
  (.setTexture sprite tex))
