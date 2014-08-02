(ns tixi.google-analytics)

(defn event! [category action]
  (when (aget js/window "ga")
    (js/ga "send" "event" category action)))
