(ns tixi.schemas
 (:require [schema.core :as s]))

(def ^:private item-types
  [:line :rect :rect-line])

(def ItemData
  {:origin [s/Int]
   :dimensions [s/Int]
   :points s/Any
   :text s/Str})

(def Item
  {:type (apply s/enum item-types)
   :input [s/Int]
   :cache (s/maybe ItemData)})

(def Data
  {:current (s/maybe {:id s/Int :item Item})
   :completed {s/Int Item}
   :tool (apply s/enum (conj item-types :select))
   :action (s/maybe s/Keyword)
   :autoincrement s/Int
   :selection {:ids [s/Int]
               :edges [s/Int]
               :rel-sizes {s/Int [s/Num]}}
   :hover-id (s/maybe s/Int)})

(def Points
  {:origin [s/Int]
   :dimensions [s/Int]
   :points [s/Int]})
