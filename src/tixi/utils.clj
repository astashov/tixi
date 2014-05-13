(ns tixi.utils)

(defmacro b
  ([f]
  `(tixi.utils/benchmark (fn [] ~f)))

  ([msg f]
  `(tixi.utils/benchmark ~msg (fn [] ~f))))
