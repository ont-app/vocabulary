(ns vocabulary.browser
  (:require [doo.runner :refer-macros [doo-tests]]
            [vocabulary.core-test]))

(doo-tests 'vocabulary.core-test)
