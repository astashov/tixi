(ns tixi.schemas
  (:require [schema.core :as s]
            [tixi.geometry :refer [Rect Point Size]]))

(def ^:private item-types
  [:line :rect :rect-line])

(def ItemData
  {:origin Point
   :dimensions Size
   :points {Point s/Str}
   :text s/Str})

(def Item
  {:type (apply s/enum item-types)
   :input Rect
   :cache (s/maybe ItemData)})

(def Data
  {:current (s/maybe {:id s/Int :item Item})
   :state s/Any
   :tool (apply s/enum (conj item-types :select))
   :action (s/maybe s/Keyword)
   :autoincrement s/Int
   :selection {:ids [s/Int]
               :rect (s/maybe Rect)
               :current (s/maybe Rect)
               :rel-rects {s/Int Rect}}
   :hover-id (s/maybe s/Int)})

(def Points
  {:origin Rect
   :dimensions Size
   :points {Point s/Str}})
