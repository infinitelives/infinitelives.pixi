(ns infinitelives.pixi.pixelfont
  (:require [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.resources :as r]))

(defn load-pixel-font
  [pixel-font-name texture-key layout]
  (.log js/console (str pixel-font-name))
  (.log js/console (str texture-key))
  (.log js/console (str layout))
  )

(defn make-text [font-key text]
  (.log js/console (str font-key " => " text))

)
