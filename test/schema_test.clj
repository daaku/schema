(ns schema-test
  (:require [clojure.test :refer [deftest is]]
            [schema :as sc]))

(defmacro S [validator value]
  `(is (= ~value (~validator ~value))))

(defmacro SV [validator input output]
  `(is (= ~output (~validator ~input))))

(defmacro E [validator value]
  `(is (sc/error? (~validator ~value))))

(defmacro EV [validator value error]
  `(is (= ~error (sc/error-value (~validator ~value)))))

(deftest test-error?
  (is (sc/error? (sc/error :foo)))
  (is (sc/error? (sc/error "bar")))
  (is (sc/error? (sc/error 42)))
  (is (sc/error? (sc/error {:answer 42})))
  (is (not (sc/error? :foo)))
  (is (not (sc/error? "bar")))
  (is (not (sc/error? 42)))
  (is (not (sc/error? {:answer 42})))
  (is (not (sc/error? {:error 42}))))

(deftest test-comp
  (S (sc/comp (sc/required) (sc/string)) "foo")
  (S (sc/comp (sc/optional) (sc/string)) nil)
  (EV (sc/comp (sc/required) (sc/string)) nil "This must be provided.")
  (EV (sc/comp (sc/required) (sc/string)) 42 "This must be a string."))

(def address-schema
  (sc/schema {:street [(sc/optional) (sc/string)]
              :country [(sc/required) (sc/in #{"IN"})]}))

(def person-fields
  {:name [(sc/required) (sc/string)]
   :age [(sc/required) (sc/number)]
   :gender [(sc/required) (sc/in #{:male :female})]
   :email [(sc/optional)]
   :address [(sc/optional) address-schema]})

(deftest test-schema-extra-fail
  (let [s (sc/schema person-fields {:extra :fail})]
    (S s {:name "yoda" :age 842 :gender :male})
    (S s {:name "yoda" :age 842 :gender :male :email "a@b.c"})
    (S s {:name "yoda" :age 842 :gender :male :address {:country "IN"}})
    (S s {:name "yoda" :age 842 :gender :male :address {:country "IN"}})
    (EV s {} {:name "This must be provided."
              :age "This must be provided."
              :gender "This must be provided."})
    (EV s
        {:name "yoda" :age 842 :gender :male :phone "123"}
        {:phone "This field was not expected."})
    (EV s
        {:name "yoda" :age 842 :phone "123"}
        {:gender "This must be provided."
         :phone "This field was not expected."})
    (EV s
        {:name "yoda" :age 842 :gender :male :address {:country "US"}}
        {:address {:country "This is not one of the allowed values."}})
    (EV s
        {:name "yoda" :age 842 :gender :male :address "US"}
        {:address {:schema/group
                   ["This was expected to be a group of fields."]}})))

(deftest test-schema-extra-pass
  (let [s (sc/schema {:name [(sc/required)]} {:extra :pass})]
    (S s {:name "yoda" :age 842})
    (E s {})))

(deftest test-schema-extra-drop
  (let [s (sc/schema {:name [(sc/required)]} {:extra :drop})]
    (SV s {:name "yoda" :age 842} {:name "yoda"})
    (E s {})))

(deftest test-required
  (S (sc/required) 42)
  (E (sc/required) nil))

(deftest test-optional
  (S (sc/optional) 42)
  (S (sc/optional) nil))

(deftest test-default
  (S (sc/default :a) 42)
  (SV (sc/default :a) nil :a))

(deftest test-string
  (S (sc/string) "foo")
  (E (sc/string) nil)
  (E (sc/string) 42))

(deftest test-number
  (S (sc/number) 1)
  (S (sc/number) 1.1)
  (E (sc/number) nil)
  (E (sc/number) "42"))

(deftest test-int
  (S (sc/int) 1)
  (S (sc/int) 0)
  (E (sc/int) nil)
  (E (sc/int) "42")
  (E (sc/int) 42.0))

(deftest test-long
  (S (sc/long) (long 1))
  (S (sc/long) (long 0))
  (E (sc/long) nil)
  (E (sc/long) "42")
  (E (sc/long) 42.0))

(deftest test-float
  (S (sc/float) (float 1.0))
  (S (sc/float) (float 0.0))
  (E (sc/float) nil)
  (E (sc/float) "42")
  (E (sc/float) 42))

(deftest test-double
  (S (sc/double) (double 1.0))
  (S (sc/double) (double 0.0))
  (E (sc/double) nil)
  (E (sc/double) "42")
  (E (sc/double) 42))

(deftest test-boolean
  (S (sc/boolean) true)
  (S (sc/boolean) false)
  (E (sc/boolean) nil)
  (E (sc/boolean) "true"))

(deftest test-set
  (S (sc/set) #{})
  (S (sc/set) #{:a})
  (E (sc/set) nil)
  (E (sc/set) "hello")
  (E (sc/set) [:a])
  (E (sc/set) {:a 1}))

(deftest test-keyword
  (S (sc/keyword) :a)
  (E (sc/keyword) nil)
  (E (sc/keyword) "keyword"))

(deftest test-vector
  (S (sc/vector) [])
  (E (sc/vector) nil)
  (E (sc/vector) "vector"))

(deftest test-in-set
  (let [in-s (sc/in #{:a "b" 3})]
    (S in-s :a)
    (S in-s "b")
    (S in-s 3)
    (E in-s nil)
    (E in-s 42)))

(deftest test-in-map
  (let [in-s (sc/in {:a true "b" true 3 true})]
    (S in-s :a)
    (S in-s "b")
    (S in-s 3)
    (E in-s nil)
    (E in-s 42)))

(deftest test-to-string
  (SV (sc/to-string) "a" "a")
  (SV (sc/to-string) nil "")
  (SV (sc/to-string) 1 "1")
  (SV (sc/to-string) 1.1 "1.1")
  (SV (sc/to-string) false "false"))

(deftest test-to-int
  (SV (sc/to-int) "1" 1)
  (SV (sc/to-int) "-42" -42)
  (E (sc/to-int) "")
  (E (sc/to-int) false))

(deftest test-to-float
  (SV (sc/to-float) "1" (float 1.0))
  (SV (sc/to-float) "-42" (float -42.0))
  (SV (sc/to-float) "-42.1" (float -42.1))
  (E (sc/to-float) "")
  (E (sc/to-float) false))

(deftest test-to-double
  (SV (sc/to-double) "1" (double 1.0))
  (SV (sc/to-double) "-42" (double -42.0))
  (SV (sc/to-double) "-42.1" (double -42.1))
  (E (sc/to-double) "")
  (E (sc/to-double) false))

(deftest test-to-boolean
  (S (sc/to-boolean) true)
  (SV (sc/to-boolean) "true" true)
  (SV (sc/to-boolean) "yes" true)
  (SV (sc/to-boolean) "Enable" true)
  (SV (sc/to-boolean) "ON" true)
  (SV (sc/to-boolean) "1" true)
  (S (sc/to-boolean) false)
  (SV (sc/to-boolean) "false" false)
  (SV (sc/to-boolean) "no" false)
  (SV (sc/to-boolean) "Disable" false)
  (SV (sc/to-boolean) "OFF" false)
  (SV (sc/to-boolean) 1 true)
  (SV (sc/to-boolean) 0 false)
  (E (sc/to-boolean) 2)
  (E (sc/to-boolean) :a)
  (E (sc/to-boolean) [1])
  (E (sc/to-boolean) "2")
  (E (sc/to-boolean) "a"))

(deftest test-to-vector
  (SV (sc/to-vector) 1 [1])
  (SV (sc/to-vector) "a" ["a"])
  (S (sc/to-vector) ["a"])
  (S (sc/to-vector) [false])
  (SV (sc/to-vector (sc/optional)) [1 nil] [1])
  (SV (sc/to-vector [(sc/to-int) (sc/greater-than 42)]) "43" [43])
  (E (sc/to-vector [(sc/to-int) (sc/greater-than 42)]) "41")
  (E (sc/to-vector (sc/string)) 1)
  (E (sc/to-vector (sc/string)) ["a" 1]))

(deftest test-to-set
  (SV (sc/to-set) 1 #{1})
  (SV (sc/to-set) "a" #{"a"})
  (SV (sc/to-set) ["a"] #{"a"})
  (SV (sc/to-set) ["a" "a"] #{"a"})
  (SV (sc/to-set) [false] #{false})
  (SV (sc/to-set (sc/optional)) [1 nil] #{1})
  (SV (sc/to-set [(sc/to-int) (sc/greater-than 42)]) "43" #{43})
  (E (sc/to-set [(sc/to-int) (sc/greater-than 42)]) "41")
  (E (sc/to-set (sc/string)) 1)
  (E (sc/to-set (sc/string)) ["a" 1]))

(deftest test-positive
  (S (sc/positive) 1)
  (E (sc/positive) 0)
  (E (sc/positive) -1))

(deftest test-zero-or-positive
  (S (sc/zero-or-positive) 1)
  (S (sc/zero-or-positive) 0)
  (E (sc/zero-or-positive) -1))

(deftest test-negative
  (S (sc/negative) -1)
  (E (sc/negative) 0)
  (E (sc/negative) 1))

(deftest test-range
  (S (sc/range 0 5) 0)
  (S (sc/range 0 5) 5)
  (S (sc/range 0 5) 2.5)
  (E (sc/range 0 5) -1)
  (E (sc/range 0 5) 6))

(deftest test-greater-than
  (S (sc/greater-than 0) 1)
  (E (sc/greater-than 0) 0)
  (E (sc/greater-than 0) -1))

(deftest test-greater-or-equal
  (S (sc/greater-or-equal 0) 1)
  (S (sc/greater-or-equal 0) 0)
  (E (sc/greater-or-equal 0) -1))

(deftest test-less-than
  (S (sc/less-than 0) -1)
  (E (sc/less-than 0) 0)
  (E (sc/less-than 0) 1))

(deftest test-less-or-equal
  (S (sc/less-or-equal 0) -1)
  (S (sc/less-or-equal 0) 0)
  (E (sc/less-or-equal 0) 1))

(deftest test-re-match
  (S (sc/re-match #"^[ab]$") "a")
  (S (sc/re-match #"^[ab]$") "b")
  (S (sc/re-match #"^[ab]$") "b")
  (E (sc/re-match #"^[ab]$") "c")
  (E (sc/re-match #"^[ab]$") 1))

(deftest test-trim
  (SV (sc/trim) " a   " "a")
  (SV (sc/trim) " a   \r\n" "a")
  (S (sc/trim) "a"))

(deftest test-non-empty
  (S (sc/non-empty) "a")
  (S (sc/non-empty) [1])
  (E (sc/non-empty) "")
  (E (sc/non-empty) []))
