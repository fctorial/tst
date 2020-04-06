# tst

[![Clojars Project](https://img.shields.io/clojars/v/fctorial/tst.svg)](https://clojars.org/fctorial/tst)

### Leiningen/Boot

    [fctorial/tst "0.2.0"]

### Clojure CLI/deps.edn

    fctorial/tst {:mvn/version "0.2.0"}

### Gradle

    compile 'fctorial:tst:0.2.0'

### Maven

```xml
<dependency>
  <groupId>fctorial</groupId>
  <artifactId>tst</artifactId>
  <version>0.2.0</version>
</dependency>
```

## Usage:

```clojure
(ns usage
  (:require [tst.core :as tst]
            [tst.predicates :as ps]))

; 'testing' returns a test spec that can be passed to 'run-test'
(def t (tst/testing :a
                    (tst/testing :b
                                 (ps/eq 1 1))
                    (tst/testing :c
                                 (ps/eq 1 2))
                    (ps/is (contains? #{1 2 3} 2))))

(def res (tst/run-test t))
; returns:
#_{:a {:b {:main {:result :OK, :context [(ps/eq 1 1)]}},
       :c {:main {:result :ERR, :message "equality test failed", :val1 1, :val2 2, :context [(ps/eq 1 2)]}},
       :main {:result :OK, :context [(ps/is (contains? #{1 3 2} 2))]}}}

; test suits can be combined:

(defn int-tests []
  (tst/testing :ints
               (tst/testing :adding_1
                            (ps/eq (+ 5 1) 6))
               (tst/testing :adding_2
                            (ps/eq (+ 5 2) 6))))

(defn string-tests []
  (tst/testing :strings
               (tst/testing :concating_asdf
                            (ps/eq (str "hello" "asdf") "helloasdf"))))

(defn all-tests []
  (tst/combine-tests [(int-tests)
                      (string-tests)]))

(def res2 (tst/run-test (all-tests)))
#_{:ints {:adding_1 {:main {:result :OK, :context [(ps/eq (+ 5 1) 6)]}},
          :adding_2 {:main {:result :ERR, :message "equality test failed", :val1 7, :val2 6, :context [(ps/eq (+ 5 2) 6)]}}},
   :strings {:concating_asdf {:main {:result :OK, :context [(ps/eq (str "hello" "asdf") "helloasdf")]}}}}

; Easy filtering
(def failed (tst/filter-tests-by-result res2 :ERR))
(def errors (tst/filter-tests-by-result res2 :EXCEPTION))
(def int-res (res2 :ints))

; summary
(println (tst/summarize-result res2))
```

See https://github.com/fctorial/parse_struct/blob/2aff96a4ab2bce8d689437b04b3e59cd16e8d216/test/all_tests.clj for more examples.

## Predicates

Body of a `testing` block should return a record of this format:

```clojure
{:result :OK  ; /:ERR
 :message "string"}
```

Along with other data concerning the unit test.

An uncaught exception will produce this result:

```clojure
{:result :EXCEPTION
 :exception e}
```