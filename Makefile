
## Update dependencies as needed....
outdated:
	clojure -M:outdated

## Testing ....
.PHONY: test-jvm
test-jvm:
	clojure -T:build test

.PHONY: test-js
test-js:
	clojure -M:test-cljs

.PHONY: test-node
test-node:
	npx shadow-cljs compile node-test

.PHONY: clean-all
clean-all:
	clojure -T:build clean :include-caches? true

test-all: clean-all test-jvm test-js test-node

## Style ....
kondo:
	clojure -M:kondo --lint src

## Security audit ...
### assumes that nvd-clojure/nvd-clojure tool has been installed
### ClojureScript is problematic, define an alias called `:backend-only`
### see also https://github.com/rm-hull/nvd-clojure
nvd:
	#clojure -J-Dclojure.main.report=stderr -Tnvd nvd.task/check :classpath \"$(shell clojure -Spath)\" :config-filename \"./.nvd/config.edn\"
	clojure -J-Dclojure.main.report=stderr -Tnvd nvd.task/check :classpath \""$(shell clojure -Spath -A:backend-only)\"" :config-filename \"".nvd/config.edn"\"

## Generate documentation ...
codox:
	clojure -X:codox

## Publishing...
uberjar:
	clojure -T:build ci

install: uberjar
	clojure -T:build install

deploy: uberjar
	clojure -T:build deploy
