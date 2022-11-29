# Contributing

## The `master` and `develop` branches

Each repo in ont-app which is not 'under contruction' has two branches:

- _master_, which reflects the state of the _current-release_
- _develop_, which reflects the state of the _next-release_

The master branch should only be modified in ways that do not change
the behavior of the current release, mainly clarification of
documentation. Changes to documenation on the master branch should
apply only to the current release.

Any future development should work off of the develop branch.  I will
endeavor to synchronize the develop branch with a clojars SNAPSHOT of
the next version (as indicated in _project.clj_). Features of this
snapshot may or may not be comprehensively tested, or documented
accurately, but they should 'mostly work'.

## Pull requests

This is a good tutorial for submitting PRs:

https://kbroman.org/github_tutorial/pages/fork.html

To avoid wasted duplication of effort, please make sure there is an
issue dedicated to the change you are looking to implement, and
use the issue's messaging utility to discuss it in advance. 

The preferred sequence of events would be:
- A draft of documentation relating to the change (in the issue)
- Sketches of the pertinent test(s) (in the issue)
- The actual PR, which would include finalized versions of the two
  points above.

## Developer notes

There is a build.clj 'tool' file defined for deps.edn specifications.


For help:

```
clojure -A:deps -T:build help/doc
```

The Makefile has targets for most of the usual stuff.

| `make` target | Description |
| --- | --- |
| outdated | Checks for outdated dependencies | 
| test-jvm | Tests the clj(c) code under the JVM |
| test-js | Tests the cljc/s code under Javascript |
| test-node | Tests the cljc/s code under Node using shadow-cljs |
| test-all | Runs all the tests |
| clean-all | Cleans output directories and caches |
| kondo | Runs Borkdude's famous linter |
| nvd | Does a security audit of your code |
| codox | Generates documentation files |
| uberjar | Creates a new uberjar |
| install | Installs a jar in your local maven |
| deploy | Deploys to clojars |


It presumes that your `~/.clojure/deps.edn` has aliases as follows:

```
{
 :aliases {
           ;; Borkdude's Kondo linter
           ;; typical usage:
           ;; clojure -M:kondo --lint src
           ;; For help: clojure -M:kondo  --help
           :kondo
           {:extra-deps {clj-kondo/clj-kondo {:mvn/version "RELEASE"}}
            :main-opts  ["-m" "clj-kondo.main"]
            }
           ;; Document generator
           ;; Call with clojure -X:codox
           :codox
           {
            :extra-deps {codox/codox {:mvn/version "0.10.8"}}
            :exec-fn codox.main/generate-docs
            :exec-args {:output-path "doc"}
            }
           ;; outdated
           ;; c.f. Lein ancient
           ;; clojure -M:outdated --help for help
           ;; Typical usage: clojure -M:outdated
           :outdated
           {
            ;; Note that it is `:deps`, not `:extra-deps`
            :deps {com.github.liquidz/antq {:mvn/version "RELEASE"}}
            :main-opts ["-m" "antq.core"]
            }
           } ;; /end aliases

}
```

The `nvd` target presumes installation of https://github.com/rm-hull/nvd-clojure

## Code of conduct

Be nice.

To clarify, please follow [these
guidelines](https://www.contributor-covenant.org/version/2/0/code_of_conduct/).

For complaints, please contact Eric Scott at {first-name}.{last-name}@acm.org.


