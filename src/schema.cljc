(ns schema
  (:refer-clojure :exclude [comp boolean set keyword vector float int double
                            long range re-matcher])
  (:require [clojure.core :as core]
            [clojure.set :as cjset]
            [clojure.string :as cjstr]))

(defn error
  "Use this function to return an error on validation failure."
  [v]
  {::error v})

(defn error?
  "Returns true if `v` contains an error."
  [v]
  (and (map? v) (contains? v ::error)))

(defn error-value
  "Returns the error value from `v`."
  [v]
  (::error v))

(defn- nil-or-error? [v]
  (or (nil? v) (error? v)))

;;
;; Validators
;;

(defn comp
  "Compose multiple validators together."
  [& fns]
  (fn [v]
    (loop [v v
           fns fns]
      (if (empty? fns)
        v
        (let [next (first fns)
              new-v (next v)]
          (if (nil-or-error? new-v)
            new-v
            (recur new-v (rest fns))))))))

(defn- schema-reduce-known [m input]
  (reduce
   (fn [[errors final] [key validator]]
     (let [new-v (validator (key input))]
       (cond
         (nil? new-v) [errors final]
         (error? new-v) [(assoc errors key (error-value new-v))
                         final]
         :else [errors (assoc final key new-v)])))
   [{} {}] m))

(defn- schema-extra-diff [m input]
  (cjset/difference (core/set (keys input)) (core/set (keys m))))

(defn- schema-success-extra [m extra msg final input]
  (case extra
    :drop final
    :pass (reduce
           (fn [final [k v]]
             (if (contains? m k)
               final
               (assoc final k v)))
           final input)
    :fail (let [diff (schema-extra-diff m input)]
            (if (empty? diff)
              final
              (error (into {} (map (fn [k] [k msg])) diff))))))

(defn- schema-error-extra [m extra msg errors input]
  (case extra
    :drop errors
    :pass errors
    :fail (let [diff (schema-extra-diff m input)]
            (if (empty? diff)
              errors
              (into errors (map (fn [k] [k msg])) diff)))))

(defn schema
  "Schema takes a map of keys to validators. Optionally `:extra` can be
  configured to one of `:fail` (the default), `:drop` or `:pass` to
  indicate the behavior of extra keys."
  ([m] (schema m nil))
  ([m {:keys [extra msg] :or {extra :fail
                              msg "This field was not expected."}}]
   (let [m (into {} (map (fn [[k v]] [k (apply comp v)])) m)]
     (fn [input]
       (if (map? input)
         (let [[errors final] (schema-reduce-known m input)]
           (if (empty? errors)
             (schema-success-extra m extra msg final input)
             (error (schema-error-extra m extra msg errors input))))
         (error {::group ["This was expected to be a group of fields."]}))))))

(defn required
  "Indicates the field is required."
  ([] (required nil))
  ([{:keys [msg] :or {msg "This must be provided."}}]
   (fn [v]
     (if (some? v)
       v
       (error msg)))))

(defn optional
  "Indicates the field is optional."
  []
  identity)

(defn default
  "Provides a default value to be used when the provided value is nil."
  [d]
  (fn [v]
    (if (nil? v) d v)))

(defn string
  "Indicates the field must be a string."
  ([] (string nil))
  ([{:keys [msg] :or {msg "This must be a string."}}]
   (fn [v]
     (if (string? v)
       v
       (error msg)))))

(defn number
  "Indicates the field must be a number."
  ([] (number nil))
  ([{:keys [msg] :or {msg "This must be a number."}}]
   (fn [v]
     (if (number? v)
       v
       (error msg)))))

(defn int
  "Indicates the field must be a integer."
  ([] (int nil))
  ([{:keys [msg] :or {msg "This must be a integer."}}]
   (fn [v]
     (if (int? v)
       v
       (error msg)))))

(defn long
  "Indicates the field must be a long."
  ([] (long nil))
  ([{:keys [msg] :or {msg "This must be a long."}}]
   (fn [v]
     (if (instance? Long v)
       v
       (error msg)))))

(defn float
  "Indicates the field must be a float."
  ([] (float nil))
  ([{:keys [msg] :or {msg "This must be a float."}}]
   (fn [v]
     (if (float? v)
       v
       (error msg)))))

(defn double
  "Indicates the field must be a double."
  ([] (double nil))
  ([{:keys [msg] :or {msg "This must be a double."}}]
   (fn [v]
     (if (double? v)
       v
       (error msg)))))

(defn boolean
  "Indicates the field must be a boolean."
  ([] (boolean nil))
  ([{:keys [msg] :or {msg "This must be a boolean."}}]
   (fn [v]
     (if (boolean? v)
       v
       (error msg)))))

(defn set
  "Indicates the field must be a set."
  ([] (set nil))
  ([{:keys [msg] :or {msg "This must be a set."}}]
   (fn [v]
     (if (set? v)
       v
       (error msg)))))

(defn keyword
  "Indicates the field must be a keyword."
  ([] (keyword nil))
  ([{:keys [msg] :or {msg "This must be a keyword."}}]
   (fn [v]
     (if (keyword? v)
       v
       (error msg)))))

(defn vector
  "Indicates the field must be a vector."
  ([] (vector nil))
  ([{:keys [msg] :or {msg "This must be a vector."}}]
   (fn [v]
     (if (vector? v)
       v
       (error msg)))))

(defn in
  "Indicates the field must be contained as defined by `contains?` in the
  collection provided as `in`."
  ([coll] (in coll nil))
  ([coll {:keys [msg]
          :or {msg "This is not one of the allowed values."}}]
   (fn [v]
     (if (contains? coll v)
       v
       (error msg)))))

(defn to-string
  "Apply `str` to input."
  []
  str)

(defn to-int
  "Parse strings using `Integer/parseInt` and cast other values using `int`."
  ([] (to-int nil))
  ([{:keys [msg] :or {msg "This value could not be converted to an integer."}}]
   (fn [v]
     (if (string? v)
       (try
         (Integer/parseInt v)
         (catch NumberFormatException _
           (error msg)))
       (try
         (core/int v)
         (catch ClassCastException _
           (error msg)))))))

(defn to-float
  "Parse strings using `Float/parseInt` and cast other values using `float`."
  ([] (to-float nil))
  ([{:keys [msg] :or {msg "This value could not be converted to a float."}}]
   (fn [v]
     (if (string? v)
       (try
         (Float/parseFloat v)
         (catch NumberFormatException _
           (error msg)))
       (try
         (core/float v)
         (catch ClassCastException _
           (error msg)))))))

(defn to-double
  "Parse strings using `Double/parseDouble` and cast other values using
  `double`."
  ([] (to-double nil))
  ([{:keys [msg] :or {msg "This value could not be converted to a double."}}]
   (fn [v]
     (if (string? v)
       (try
         (Double/parseDouble v)
         (catch NumberFormatException _
           (error msg)))
       (try
         (core/double v)
         (catch ClassCastException _
           (error msg)))))))

(defn to-boolean
  "Parse strings using the following rules:
    `true | yes | enable | on | 1 => true`
    `false | no | disable | off | 0 => false`

  Parse numbers using the following rules:
    `1 => true`
    `0 => false`

  Other values are unacceptable."
  ([] (to-boolean nil))
  ([{:keys [msg] :or {msg "This value could not be converted to a boolean."}}]
   (fn [v]
     (cond
       (boolean? v)
       v

       (string? v)
       (case (cjstr/lower-case v)
         ("true" "yes" "enable" "on" "1") true
         ("false" "no" "disable" "off" "0") false
         (error msg))

       (number? v)
       (case v
         1 true
         0 false
         (error msg))

       :else
       (error msg)))))

(defn to-vector
  "Convert a collection or a single value into a vector. Optionally applies
  the given schema to each value."
  ([] (to-vector identity))
  ([sc]
   (let [sc (core/comp (map (if (vector? sc) (apply comp sc) sc))
                       (filter some?))]
     (fn [v]
       (let [out (into [] sc (if (coll? v) v [v]))]
         (if-let [errors (first (filter error? out))]
           errors
           out))))))

(defn to-set
  "Convert a collection or a single value into a set. Optionally applies
  the given schema to each value."
  ([] (to-set identity))
  ([sc]
   (let [sc (core/comp (map (if (vector? sc) (apply comp sc) sc))
                       (filter some?))]
     (fn [v]
       (let [out (into #{} sc (if (coll? v) v [v]))]
         (if-let [errors (first (filter error? out))]
           errors
           out))))))

(defn positive
  "Ensure the value is positive checked by `pos?`."
  ([] (positive nil))
  ([{:keys [msg] :or {msg "This value must be positive."}}]
   (fn [v]
     (if (pos? v)
       v
       (error msg)))))

(defn zero-or-positive
  "Ensure the value is zero or positive checked by `pos?`."
  ([] (zero-or-positive nil))
  ([{:keys [msg] :or {msg "This value must be zero or positive."}}]
   (fn [v]
     (if (or (zero? v) (pos? v))
       v
       (error msg)))))

(defn negative
  "Ensure the value is negative checked by `neg?`."
  ([] (negative nil))
  ([{:keys [msg] :or {msg "This value must be negative."}}]
   (fn [v]
     (if (neg? v)
       v
       (error msg)))))

(defn range
  "Ensure the value is the given range, start and end inclusive. Comparisons
  are made using `compare`."
  ([start end] (range start end nil))
  ([start end {:keys [msg]
               :or {msg "This value is outside the allowed range."}}]
   (fn [v]
     (if (or (= (compare start v) 1) (= (compare end v) -1))
       (error msg)
       v))))

(defn greater-than
  "Ensure the value is greater than the provided value. Comparisons are made
  using `compare`."
  ([target] (greater-than target nil))
  ([target {:keys [msg]
            :or {msg "This value is less than the allowed value."}}]
   (fn [v]
     (if (= (compare v target) 1)
       v
       (error msg)))))

(defn greater-or-equal
  "Ensure the value is greater than or equal to the provided value.
  Comparisons are made using `compare`."
  ([target] (greater-or-equal target nil))
  ([target {:keys [msg]
            :or {msg "This value is less than the allowed value."}}]
   (fn [v]
     (if (= (compare v target) -1)
       (error msg)
       v))))

(defn less-than
  "Ensure the value is less than the provided value. Comparisons are made
  using `compare`."
  ([target] (less-than target nil))
  ([target {:keys [msg]
            :or {msg "This value is more than the allowed value."}}]
   (fn [v]
     (if (= (compare target v) 1)
       v
       (error msg)))))

(defn less-or-equal
  "Ensure the value is less than or equal to the provided value.
  Comparisons are made using `compare`."
  ([target] (less-or-equal target nil))
  ([target {:keys [msg]
            :or {msg "This value is more than the allowed value."}}]
   (fn [v]
     (if (= (compare target v) -1)
       (error msg)
       v))))

(defn re-match
  "Ensure the value matches the provided regular expression. If the value is
  not a string, it is considered not to match."
  ([re] (re-match re nil))
  ([re {:keys [msg]
        :or {msg "This value does not match the expected pattern."}}]
   (fn [v]
     (if (and (string? v) (re-find re v))
       v
       (error msg)))))

(defn trim
  "Trims leading and trailing whitespace."
  []
  cjstr/trim)

(defn non-empty
  "Ensures the value is non-empty."
  ([] (non-empty nil))
  ([{:keys [msg] :or {msg "This value cannot be empty."}}]
   (fn [v]
     (if (empty? v)
       (error msg)
       v))))
