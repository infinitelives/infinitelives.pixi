(ns infinitelives.pixi.macros)

(defmacro ^:private assert-args
  [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                 (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
       ~(let [more (nnext pairs)]
          (when more
            (list* `assert-args more)))))

(defmacro get-layer [canvas layer]
  `(or
    (-> ~canvas :layer ~layer)
    (throw (js/Error (str "canvas layer " ~layer " not found.")))
    ))

(defmacro with-sprite* [canvas layer bindings & body]
  (assert-args
   (vector? bindings) "a vector for its binding"
   (even? (count bindings)) "an even number of forms in binding vector")

  (if (pos? (count bindings))
    (let [symb (first bindings) val (second bindings)]
      `(let [parent# (if (keyword? ~layer) (get-layer ~canvas ~layer) ~layer)
             ~symb ~val]
         (try (.addChild parent# ~symb)
              (with-sprite ~canvas ~layer ~(subvec bindings 2) ~@body)
              (finally (.removeChild parent# ~symb)))))
    `(do ~@body)))

(defmacro with-sprite [& body]
  (cond
    (vector? (first body))
    `(with-sprite*
       (infinitelives.pixi.canvas/get-default-canvas) (infinitelives.pixi.canvas/get-default-layer)
       ~(first body)
       ~@(next body))

    (vector? (second body))
    `(with-sprite*
       (infinitelives.pixi.canvas/get-default-canvas) ~(first body)
       ~(second body)
       ~@(nnext body))

    :default
    `(with-sprite* ~@body)))

(comment
  (macroexpand-1 '(with-sprite [a (ms)] a b c)))

(defmacro with-layered-sprite [bindings & body]
  (assert-args
   (vector? bindings) "a vector for its binding"
   (= 0 (mod (count bindings) 3)) "triplets of forms in binding vector")

  (if (pos? (count bindings))
    (let [symb (first bindings)
          layer (second bindings)
          val (nth bindings 2)]
      `(let [~symb ~val
             canvas# (infinitelives.pixi.canvas/get-default-canvas)]
         (try (.addChild
               (get-layer canvas# ~layer)
               ~symb)
              (with-layered-sprite ~(subvec bindings 3) ~@body)
              (finally (.removeChild
                        (get-layer canvas# ~layer)
                        ~symb)))))
    `(do ~@body)))

(defmacro with-sprite-set* [canvas layer bindings & body]
  (assert-args
   (vector? bindings) "a vector for its binding"
   (even? (count bindings)) "an even number of forms in binding vector")

  (if (pos? (count bindings))
    (let [symb (first bindings) val (second bindings)]
      `(let [~symb ~val]
         (try
           (doseq [sprite# ~symb]
             (.addChild (get-layer ~canvas ~layer) sprite#))
           (with-sprite-set ~canvas ~layer ~(subvec bindings 2) ~@body)
           (finally (doseq [sprite# ~symb]
                      (.removeChild (get-layer ~canvas ~layer) sprite#))))))
    `(do ~@body)))

(defmacro with-sprite-set [& body]
  (cond
    (vector? (first body))
    `(with-sprite-set*
       (infinitelives.pixi.canvas/get-default-canvas) (infinitelives.pixi.canvas/get-default-layer)
       ~(first body)
       ~@(next body))

    (vector? (second body))
    `(with-sprite-set*
       (infinitelives.pixi.canvas/get-default-canvas) ~(first body)
       ~(second body)
       ~@(nnext body))

    :default
    `(with-sprite-set* ~@body)))


(defmacro while-let
    "Repeatedly executes body while test expression is true, evaluating the body with binding-form bound to the value of test."
    [bindings & body]
    (let [form (first bindings) test (second bindings)]
        `(loop [~form ~test]
             (when ~form
                 ~@body
                 (recur ~test)))))
