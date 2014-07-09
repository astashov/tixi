(ns tixi.controller)

(defmacro render [& f]
  `(tixi.controller/-render (fn [] (do ~@f))))
