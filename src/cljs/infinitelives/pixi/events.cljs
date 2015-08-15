(ns infinitelives.pixi.events
  (:require [cljs.core.async :refer [put! chan close!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;;
;; Animation handler
;;
(def fallback-fps 60)

(defn make-request-animation-frame
  "compose a function that is the r-a-f func. returns a function. This returned function takes a callback and ensures
  its called next frame"
  []
  (cond
   (.-requestAnimationFrame js/window)
   #(.requestAnimationFrame js/window %)

   (.-webkitRequestAnimationFrame js/window)
   #(.webkitRequestAnimationFrame js/window %)

   (.-mozRequestAnimationFrame js/window)
   #(.mozRequestAnimationFrame js/window %)

   (.-oRequestAnimationFrame js/window)
   #(.oRequestAnimationFrame js/window %)

   (.-msRequestAnimationFrame js/window)
   #(.msRequestAnimationFrame js/window %)

   :else
   #(.setTimeout js/window % (/ 1000 fallback-fps))))


;;
;; Resize Channel
;; --------------
;; resize channels receive [width height]
;;
(def *resize-chans* (atom #{}))

(defn new-resize-chan []
  (let [c (chan)]
    (swap! *resize-chans* conj c)
    c))

(defn del-resize-chan [c]
  (swap! *resize-chans* disj c))

(defn clear-resize-chans! []
  (swap! *resize-chans* #{}))

(defn resize-event-chan-handler [ev]
  (let [size [(.-innerWidth js/window) (.-innerHeight js/window)]]
    (doseq [c @*resize-chans*] (put! c size))))

(defn install-resize-handler
  "install the resize callback to resize the main canvas renderer"
  []
  (.addEventListener js/window "resize" resize-event-chan-handler))


;;
;; Frame Channel
;; -------------
;; frame channel receives a true every frame paint
;;
(def *frame-chans* (atom #{}))

(defn new-frame-chan []
  (let [c (chan)]
    (swap! *frame-chans* conj c)
    c))

(defn del-frame-chan! [c]
  (swap! *frame-chans* disj c))

(defn clear-frame-chans! []
  (swap! *frame-chans* #{}))

(defn frame-event-chan-handler [ev]
  (request-animation-frame frame-event-chan-handler)
  (doseq [c @*frame-chans*] (put! c true)))

(defn install-frame-handler
  "install the frame callback to send frame chan messages"
  []
  (request-animation-frame frame-event-chan-handler))


(install-frame-handler)
(install-resize-handler)
