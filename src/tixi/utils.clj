(ns tixi.utils)

(defmacro b
  ([f]
  `(tixi.utils/benchmark (fn [] ~f)))

  ([msg f]
  `(tixi.utils/benchmark ~msg (fn [] ~f))))

(defmacro defdata [name args & bodies]
  `(defn ~name
     (~args ~(concat [name `@tixi.data/data] args))
     ~(concat [(into [] (cons 'data args))] bodies)))
