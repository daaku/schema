(ns schema-test-util
  (:require [clojure.test :as t]
            [schema :as sc]))

(defmacro S [validator value]
  `(t/is (= ~value (~validator ~value))))

(defmacro SV [validator input output]
  `(t/is (= ~output (~validator ~input))))

(defmacro E [validator value]
  `(t/is (sc/error? (~validator ~value))))

(defmacro EV [validator value error]
  `(t/is (= ~error (sc/error-value (~validator ~value)))))
