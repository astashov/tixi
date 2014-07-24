(ns tixi.google-analytics)

(defn event! [category action]
  (when (.-ga js/window)
    (.ga js/window "send" "event" category action)))
