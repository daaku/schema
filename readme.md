schema
======

Schema validation and transformation, using simple functions. This is a
library of boring functions that do mundane validation and
transformation. It isn't meant to be a fancy introspective schema
definition language.

This library is compatible with [Clojure](https://clojure.org/) and
[ClojureScript](https://clojurescript.org/).

[![Documentation](https://cljdoc.org/badge/daaku/schema)](https://cljdoc.org/d/daaku/schema/CURRENT)
[![Clojars](https://img.shields.io/clojars/v/daaku/schema.svg)](https://clojars.org/daaku/schema)
[![Build](https://github.com/daaku/schema/workflows/build/badge.svg)](https://github.com/daaku/schema/actions?query=workflow%3Abuild)

Documentation: https://cljdoc.org/d/daaku/schema

## Usage

The building block for `schema` are validator functions. These take a
value, and return one of 3 things:

1. `nil` to indicate the value should be dropped, and the rest of the
   chain will not be run.
1. An error using `(error ...)` which indicates an error, and the rest
   of the chain will not be run.
1. Any other value, which will be passed to the next validator in the
   chain, if there is one.

With this in mind, a simple validator could be:

```clojure
(ns myapp
  (:require [daaku.schema :as sc]))

(defn ensure-int [v]
  (if (int? v)
    v
    (sc/error "not an int")))
```

Similarly, the pattern allows for transformation, for example:

```clojure
(defn toggle [v]
  (if (= "enable" v)
    true
    false))
```

The library provide a variety of predicates and transformers. They are
all written as functions that return a validator, with the given options
(if any).

### Schema

Of course, you usually want to work with `map`s. So here's how you would
do that:

```clojure
(def countries #{"India", "Belgium", "USA"})

(def address-schema
  (sc/schema {:street [(sc/required) (sc/string)]
              :city [(sc/optional) (sc/string)]
              :country [(sc/required) (sc/in countries)]}))

(def person-schema
  (sc/schema {:name [(sc/required) (sc/string)]
              :age [(sc/required) (sc/to-int)]
              :address [(sc/optional) address-schema]}))

(def samples [{}
              {:name "Yoda" :age "842"}
              {:name "Yoda" :age "old"}
              {:name "Yoda" :age 842 :address {}}])

(for [input samples]
  (let [validated (person-schema input)]
    (if (sc/error? validated)
      (println "error: \n" (sc/error-value validated) "\n for: \n" input)
      (println "success: \n" validated))))
```


## TODO

- [ ] transform: keyword
- [ ] type: date
- [ ] type: date time
- [ ] type: uuid
- [ ] transform: date
- [ ] transform: date time
- [ ] validate: either or key in map
- [ ] validate: at least one of key in map
