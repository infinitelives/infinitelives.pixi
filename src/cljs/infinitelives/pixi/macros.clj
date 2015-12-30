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
  `(-> ~canvas :layer ~layer))

(defmacro with-sprite [canvas layer bindings & body]
  (assert-args
   (vector? bindings) "a vector for its binding"
   (even? (count bindings)) "an even number of forms in binding vector")

  (if (pos? (count bindings))
    (let [symb (first bindings) val (second bindings)]
      `(let [~symb ~val]
         (.addChild (get-layer ~canvas ~layer) ~symb)
         (with-sprite ~canvas ~layer ~(subvec bindings 2) ~@body)
         (.removeChild (get-layer ~canvas ~layer) ~symb)))
    `(do ~@body)))

(defmacro with-sprite-set [canvas layer bindings & body]
  (assert-args
   (vector? bindings) "a vector for its binding"
   (even? (count bindings)) "an even number of forms in binding vector")

  (if (pos? (count bindings))
    (let [symb (first bindings) val (second bindings)]
      `(let [~symb ~val]
         (doseq [sprite# ~symb]
           (.addChild (get-layer ~canvas ~layer) sprite#))
         (with-sprite-set ~canvas ~layer ~(subvec bindings 2) ~@body)
         (doseq [sprite# ~symb]
           (.removeChild (get-layer ~canvas ~layer) sprite#))))
    `(do ~@body)))

(defmacro while-let
    "Repeatedly executes body while test expression is true, evaluating the body with binding-form bound to the value of test."
    [bindings & body]
    (let [form (first bindings) test (second bindings)]
        `(loop [~form ~test]
             (when ~form
                 ~@body
                 (recur ~test)))))
