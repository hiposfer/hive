# hive

Your go-to public transport routing app

## requirements

- [yarn](https://yarnpkg.com/lang/en/docs/install/)
- [lein](http://leiningen.org/#install)

## installation

``` shell
# install js modules
yarn install

# signup using exp CLI
yarn run exp signup

# start the figwheel server and cljs repl 
lein figwheel
```

in another terminal
``` shell
## Start exponent server
yarn run exp start
```

## how to
- add new assets or external modules
  
``` clj
(def cljs-logo (js/require "./assets/images/cljs.png"))
(def FontAwesome (js/require "@expo/vector-icons/FontAwesome"))
```
now reload simulator or device

## Notes
- make sure you disable live reload from the Developer Menu, also turn off Hot Module Reload.
Since Figwheel already does those.

- production build (generates `js/externs.js` and `main.js`)

``` shell
lein prod-build
```