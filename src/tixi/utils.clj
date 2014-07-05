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

(defmacro defpoly [name args & impls]
  `(defn ~name ~args
     ~(if (> (count impls) 1)
        `(condp tixi.utils/item-type ~(first args)
           ~@impls)
        `(do ~@impls))))

(defmacro defpoly- [name args & impls]
  `(defn- ~name ~args
     ~(if (> (count impls) 1)
        `(condp tixi.utils/item-type ~(first args)
           ~@impls)
        `(do ~@impls))))
