# tst

[![Clojars Project](https://img.shields.io/clojars/v/fctorial/tst.svg)](https://clojars.org/fctorial/tst)

### Leiningen/Boot

    [fctorial/tst "0.5.0"]

### Clojure CLI/deps.edn

    fctorial/tst {:mvn/version "0.5.0"}

### Gradle

    compile 'fctorial:tst:0.5.0'

### Maven

```xml
<dependency>
  <groupId>fctorial</groupId>
  <artifactId>tst</artifactId>
  <version>0.5.0</version>
</dependency>
```

## Usage:

```clj
; 'testing' macro returns test suite as a value
(def t (testing :a
                ; tests can be nested
                (testing :b
                         {:result :OK})
                ; leaf test nodes can have state attached to them
                ; which will be returned with result
                (testing :c [a b]
                         ; these symbols will be bound to atoms when running the tests
                         (reset! a 1)
                         (reset! b (/ 1 0))
                         {:result  :OK})
                {:result  :ERR
                 :message "another test"}))

; 'run-test' runs a test suite
(def res (run-test t))
; res now has this value:
#_{:a {:b {:main {:result :OK, :context (clojure.core/fn [{}] {:result :OK}), :state {}}},
       :c {:main {:result :EXCEPTION,
                  :exception #error{:cause "Divide by zero",
                                    :via [],
                                    :trace []},
                  :context (clojure.core/fn [{a (quote a), b (quote b)}] (reset! a 1) (reset! b (/ 1 0)) {:result :OK}),
                  :state {a 1, b :tst/UNINITIALIZED}}},
       :main {:result :ERR,
              :message "another test",
              :context (clojure.core/fn [{}] {:result :ERR, :message "another test"}),
              :state {}}}}
```

[Guide](test/usage.cljc)

See https://github.com/fctorial/parse_struct/blob/master/test/all_tests.clj for more examples.

