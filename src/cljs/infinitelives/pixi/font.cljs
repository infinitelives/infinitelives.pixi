(ns infinitelives.pixi.font
  (:require [cljs.core.async :refer [put! chan <! >! alts! timeout close!]]
            [infinitelives.utils.console :refer [log]]
            [infinitelives.utils.dom :as dom]
            [infinitelives.pixi.sprite :as sprite]
            [infinitelives.pixi.texture :as texture]
            [PIXI])

)

(def printable-characters
  " ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!?@#$%^&*()-=_+[]{};':\",.<>/`~\\|")

(defn load-font-js-metrics
  "returns a channel that receives the metrics hashmap
  once the font is loaded and processed by Font.js.
  url is either a url to a ttf or otf. Or a system font definition
  size is in pixels"
  [url size]
  (let [f (js/Font.)
        c (chan)]
    (set! (.-onload f)
          #(put! c
                 (into {}
                       (for [ch printable-characters]
                         [
                          ch
                          (let [metric (.measureText f ch size)]
                            {
                             :width (.-width metric)
                             :height (.-height metric)
                             :leading (.-leading metric)
                             :ascent (.-ascent metric)
                             :descent (.-descent metric)
                             :bounds (.-bounds metric)
                             })]))
                 ))
    (set! (.-onerror f) (fn [arg] (log arg) (put! c false)))
    (set! (.-src f) url)
    c))

(defn make-text
  "Make a PIXI.Text object using font and string and default settings"
  [font str &
   {:keys [fill align style weight stroke
           strokeThickness dropShadow dropShadowColor dropShadowDistance
           x y
           ]
    :or {fill "#ffffff" align "left" style "normal" weight 400 stroke "#ffffff"
         strokeThickness 0 dropShadow false dropShadowColor "#444444"
         dropShadowDistance 5}
    }
   ]
  (let [spr (PIXI/Text. str
                           #js {
                                :font font
                                :fill fill
                                :align align
                                :style style
                                :weight weight
                                :stroke stroke
                                :strokeThickness strokeThickness
                                :dropShadow dropShadow
                                :dropShadowColor dropShadowColor
                                :dropShadowDistance dropShadowDistance
                                })]
    (set! (.-anchor.x spr) 0.5)
    (set! (.-anchor.y spr) 0.5)
    (when x (set! (.-position.x spr) x))
    (when y (set! (.-position.y spr) y))
    spr
))

;; "400 48px Open Sans"
(defn font-metrics
  [font-str]
  (into
   {}
   (for [c printable-characters]
     (let [t (make-text font-str  c)]
       [c
        {
         :width (.-width t)
         :height (.-height t)
         :size [(.-width t) (.-height t)]
         :texture t
         }]))))

(defn render-texture-char!
  "render a char at position [x y] onto a RenderTexture.
  returns that RenderTexture. Mutates RenderTexture"
  [rtex metrics char x y]
  (let [contain (PIXI/DisplayObjectContainer.)
        c (-> char metrics :texture)]
    ;(.log js/console c x y)
    (set! (.-position.x c) x)
    (set! (.-position.y c) y)
    (set! (.-anchor c) (sprite/make-point 0 0))
    (.addChild contain c)
    (.render rtex contain)
    rtex))

(defn text-width [metrics text]
  (reduce + (for [c text] (:width (metrics c)))))

(defn text-height [metrics text]
  (reduce max (for [c text] (:height (metrics c)))))

(defn x-position
  "given a string, and the index of the charater to render in
  the string, return its x position of rendering (by adding up
  previous characters sizes"
  [metrics row i]
  (if (= 0 i)
    0
    (text-width metrics (subs row 0 i))))

(defn make-rowset-sizes
  [metrics chars]
  (for [charset (partition 10 chars)]
    [
     ;; width
     (reduce +
             (for [c charset] (:width (metrics c))))

     ;; height
     (reduce max
             (for [c charset] (:height (metrics c))))
     ]
    ))

(defn calculate-font-layout-size
  [rows]
  [(reduce max (for [[w h] rows] w))
   (reduce + (for [[w h] rows] h))])


(defn install-google-webfont-script! []
  (let [elem (dom/create-element :script)]
    (dom/insert-before!
     (-> elem
         (dom/set-attr! :src (str (if (= "https:" (.-location.protocol js/document)) "https" "http")
                                    "://ajax.googleapis.com/ajax/libs/webfont/1/webfont.js"))
         (dom/set-attr! :type "text/javascript")
         (dom/set-attr! :async "true")
         )
     (aget (.getElementsByTagName js/document "script") 0))))

(defn install-google-font-stylesheet! [url]
  (let [elem (dom/create-element :link)]
    (dom/insert-before!
     (-> elem
         (dom/set-attr! :rel "stylesheet")
         (dom/set-attr! :type "text/css")
         (dom/set-attr! :href url))
     (aget (.getElementsByTagName js/document "link") 0))))

(defn install-force-loading-font-div! [fontname]
  (let [el (dom/create-element :div)]
    (-> el
         (dom/set-style! :font-family fontname
                           :position "absolute"
                           :left "-50px"
                           :top "0px"
                           )
         (dom/set-text! "."))
    (dom/append!
     el
     js/document.body)))

(defn render-row [metrics texture row y]
  (for [[i c] (partition 2 (interleave (range) row))]
    (let [tex (render-texture-char!
               texture metrics
               c
               (x-position metrics row i)
               y)]
      [c [(x-position metrics row i) y]])))

(defn render-tiled-font-texture [metrics texture line-height]
  (loop [b texture
         d {}
         i 0
         chars (partition 10 printable-characters)]
    (if (not (empty? chars))
      (let [details (render-row
                     metrics
                     texture
                     (apply str
                            (first chars))
                     (* i line-height))]
        (recur
         b
         (into d details)
         (inc i)
         (rest chars)))
      d))
  )


(defn make-tiled-font [family weight size]
  (let [fullname (str weight " " size "px " family)
        metrics (font-metrics fullname)
        [w h] (-> metrics
                  (make-rowset-sizes printable-characters)
                  calculate-font-layout-size)
        texture (PIXI/RenderTexture. w h)
        line-height (text-height metrics printable-characters)
        ]
    {
     :texture texture
     :fullname fullname
     :metrics metrics
     :line-height line-height
     :positions (render-tiled-font-texture metrics texture line-height)
     })
)

(defn font-char-sub-texture [font ch]
  (texture/sub-texture
   (:texture font)
   ((:positions font) ch)
   (:size ((:metrics font) ch))))

(defn font-make-batch [font text &
                       {:keys [x y] :or {x 0 y 0} :as options}]
  (let [buff (:texture font)
        metrics (:metrics font)
        sb (PIXI/SpriteBatch. buff)

        ;; handle by center of text
        _ (set! (.-pivot.x sb) (int (/ (text-width metrics text) 2)))
        _ (set! (.-pivot.y sb) (int (/ (text-height metrics text) 2)))
        ]
    (loop [t text xp x]
      (let [c (first t)
            r (rest t)
            [w h] (:size (metrics c))
            s (sprite/make-sprite
               (font-char-sub-texture font c)
               :x xp
               :y y
               :xhandle 0
               :yhandle 0
                )]
        (.addChild sb s)
        (if (not (empty? r))
          (recur r (+ xp w))
          sb)))
    {
     :sprite sb
     :font font
    }))


(defn font-text-width [font text]
  (text-width (:metrics font) text))


(defn batch-add
  "starting at position 'pos', add all the letters so that the
  string text appears. Return the next render position for the next character."
  [batch [x y] text]
  (let [font (:font batch)
        sb (:sprite batch)
        metrics (:metrics font)]
    (loop [t text xp 0]
      (let [c (first t)
            r (rest t)
            [w h] (:size (metrics c))
            s (sprite/make-sprite
               (font-char-sub-texture font c)
               :x (+ x xp)
               :y y
               :xhandle 0
               :yhandle 0
                )]
        (log c (+ x xp) y)
        (.addChild sb s)
        (if (not (empty? r))
          (recur r (+ xp w))
          [(+ xp w x) y])))))


;; install the installer??
(install-google-webfont-script!)
