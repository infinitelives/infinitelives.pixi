(ns infinitelives.pixi.sprite
  (:require [cljsjs.pixi]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.pixi.texture :as texture]
            [infinitelives.utils.console :refer [log]]))

(def ^:dynamic *default-scale* 1)
(defn get-default-scale [] *default-scale*)
(defn set-default-scale! [scale] (set! *default-scale* scale))

(defn make-point
  "Make a PIXI Point from x and y"
  [x y]
  (js/PIXI.Point. x y))

(defn make-sprite
  "construct a sprite by its texture. optionally pass in other things"
  [texture & {:keys [x y xhandle yhandle scale alpha pos
                     rotation tint tiling tiling-width tiling-height visible
                     mousemove mousedown mouseup mouseupoutside
                     touchmove touchdown touchup touchupoutside
                     buttonmode]
              :or {pos (vec2/vec2 0 0)
                   xhandle 0.5 yhandle 0.5
                   scale *default-scale*
                   alpha 1
                   visible true
                   rotation 0
                   tiling false}}]
  (let [s (if tiling
            (js/PIXI.extras.TilingSprite.
             (if (keyword? texture) (texture/get-texture texture) texture)
             tiling-width tiling-height)
            (js/PIXI.Sprite. (if (keyword? texture) (texture/get-texture texture) texture)))]
    (assert s "creation of sprite failed and returned nil")
    (set! (.-anchor s) (make-point xhandle yhandle))
    (when pos
      (set! (.-x s) (vec2/get-x pos))
      (set! (.-y s) (vec2/get-y pos)))
    (when x
      (set! (.-x s) x))
    (when y
      (set! (.-y s) y))
    (set! (.-rotation s) rotation)
    (set! (.-visible s) visible)
    (when-not (= scale 1)
      (set! (.-scale s)
            (if (number? scale)
              (make-point scale scale)
              (make-point (get scale 0) (get scale 1)))))
    (when-not (= 1 alpha)
      (set! (.-alpha s) alpha))
    (set! (.-interactive s) (or mousemove mousedown mouseup mouseupoutside
                                touchmove touchdown touchup touchupoutside))

    (when mousedown (set! (.-mousedown s) mousedown))
    (when mousemove (set! (.-mousemove s) mousemove))
    (when mouseup (set! (.-mouseup s) mouseup))
    (when mouseupoutside (set! (.-mouseupoutside s) mouseupoutside))

    (when touchdown (set! (.-touchdown s) touchdown))
    (when touchmove (set! (.-touchmove s) touchmove))
    (when touchup (set! (.-touchup s) touchup))
    (when touchupoutside (set! (.-touchupoutside s) touchupoutside))

    (when-not (nil? buttonmode) (set! (.-buttonMode s) buttonmode))

    (when tint (set! (.-tint s) tint))
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

(defn set-anchor-x! [sprite x]
  (set! (.-anchor.x sprite) x))

(defn set-anchor-y! [sprite y]
  (set! (.-anchor.y sprite) y))

(defn set-alpha! [sprite alpha]
  (set! (.-alpha sprite) alpha))

(defn set-pivot! [sprite x y]
  (set! (.-pivot.x sprite) x)
  (set! (.-pivot.y sprite) y))

(defn get-pivot [sprite]
  (.-pivot sprite))

(defn get-pivot-x [sprite]
  (.-pivot.x sprite))

(defn get-pivot-y [sprite]
  (.-pivot.y sprite))

(defn get-pivot-xy [sprite]
  [(.-pivot.x sprite) (.-pivot.y sprite)])

(defn set-scale!
  ([sprite s]
   (set! (.-scale sprite) (make-point s s)))
  ([sprite sx sy]
   (set! (.-scale sprite) (make-point sx sy))))

(defn get-scale [sprite]
  (let [scale (.-scale sprite)]
    [(.-x scale) (.-y scale)]))

(defn set-rotation! [sprite theta]
  (set! (.-rotation sprite) theta))

(defn set-texture! [sprite tex]
  (if (keyword? tex)
    (set! (.-texture sprite) (texture/get-texture tex))
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

(defn get-rects [sprite]
  (let [bounds (.getLocalBounds sprite)]
    [(.-x bounds) (.-y bounds) (.-width bounds) (.-height bounds)]))

(defn get-edges [sprite]
  (let [neg-pivot-x (- (get-pivot-x sprite))
        neg-pivot-y (- (get-pivot-y sprite))
        [x y w h] (get-rects sprite)
        pos-x (get-x sprite)
        pos-y (get-y sprite)]
    [(+ pos-x x neg-pivot-x) (+ pos-y y neg-pivot-y)
     (+ pos-x x w neg-pivot-x) (+ pos-y y h neg-pivot-y)]))

(defn update-container-handle!
  [container xhandle yhandle]
  (let [children (.-children container)
        edges (map get-edges children)
        lefts (map first edges)
        tops (map second edges)
        rights (map #(nth % 2) edges)
        bottoms (map #(nth % 3) edges)

        left (apply min lefts)
        top (apply min tops)
        right (apply max rights)
        bottom (apply max bottoms)

        width (- right left)
        height (- bottom top)

        x-pivot (+ left (* xhandle width))
        y-pivot (+ top (* yhandle height))
        ]
    (set-pivot! container x-pivot y-pivot)
    [x-pivot y-pivot]))

(defprotocol Container
  (update-handle! [container xhandle yhandle]))

(extend-type js/PIXI.Container
  Container
  (update-handle!
    ([container xhandle yhandle]
     (update-container-handle! container xhandle yhandle))))

(extend-type js/PIXI.ParticleContainer
  Container
  (update-handle!
    ([container xhandle yhandle]
     (update-container-handle! container xhandle yhandle))))

(defn- opts->js [opts]
  (clj->js (into {} (for [o opts] [(name o) true]))))

(defn make-container
  [ & {:keys [children
              x y xhandle yhandle scale alpha pos
              rotation tint visible
              interactive
              mousemove mousedown mouseup mouseupoutside
              touchmove touchstart touchup touchupoutside
              buttonmode particle particle-opts]
       :or {children []
            pos (vec2/vec2 0 0)
            xhandle nil yhandle nil
            scale 1
            alpha 1
            visible true
            rotation 0
            particle-opts #{:position}}}]
  (let [container
        (if particle
          (js/PIXI.particles.ParticleContainer. nil (opts->js particle-opts))
          (js/PIXI.Container.))]
    (assert container "creation of container failed and returned nil")
    (when pos
      (set! (.-x container) (vec2/get-x pos))
      (set! (.-y container) (vec2/get-y pos)))
    (when x
      (set! (.-x container) x))
    (when y
      (set! (.-y container) y))
    (set! (.-rotation container) rotation)
    (set! (.-visible container) visible)
    (when-not (= scale 1)
      (set! (.-scale container)
            (if (number? scale)
              (make-point scale scale)
              (make-point (get scale 0) (get scale 1)))))
    (when-not (= 1 alpha)
      (set! (.-alpha container) alpha))
    (when tint
      (set! (.-tint container) tint))

    (set! (.-interactive container) (boolean (or interactive mousemove mousedown mouseup mouseupoutside
                                                 touchmove touchstart touchup touchupoutside)))
    (set! (.-interactiveChildren container) (boolean (or interactive mousemove mousedown mouseup mouseupoutside
                                                         touchmove touchstart touchup touchupoutside)))

    (set! (.-hitArea container) (new js/PIXI.Rectangle -1000 -1000 2000 2000))


    (when mousedown
      (set! (.-mousedown container) mousedown))
    (when mousemove (set! (.-mousemove container) mousemove))
    (when mouseup (set! (.-mouseup container) mouseup))
    (when mouseupoutside (set! (.-mouseupoutside container) mouseupoutside))

    (when touchstart (set! (.-touchstart container) touchstart))
    (when touchmove (set! (.-touchmove container) touchmove))
    (when touchup (set! (.-touchup container) touchup))
    (when touchupoutside (set! (.-touchupoutside container) touchupoutside))


    (when-not (nil? buttonmode) (set! (.-buttonMode container) buttonmode))


    (doseq [child children] (.addChild container child))
    (when (and (or xhandle yhandle) (pos? (count children)))
         (update-handle! container (or xhandle 0) (or yhandle 0)))

    container))

(defn container-transform [container pos]
  (let [p (.applyInverse (.-worldTransform container) pos)]
    [(.-x p) (.-y p)]))
