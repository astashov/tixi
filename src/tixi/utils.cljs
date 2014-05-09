(ns tixi.utils)

(defn p [value]
  (*print-fn* (pr-str value))
  value)


(defn seq-contains? [coll target]
  (some #(= target %) coll))

(defn b
  ([f] (b nil f))
  ([msg f]
  (let [start (.now js/Date)
        result (f)]
    (p (str (when msg (str msg ": ")) (- (.now js/Date) start) "ms"))
    result)))
