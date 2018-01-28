# hive

Your go-to public transport routing app

## requirements

- [yarn](https://yarnpkg.com/lang/en/docs/install/)
- [lein](http://leiningen.org/#install)

## development

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

## usage

Once you have a figwheel and exp server running. Open the
expo app in your mobile device and scan the QR code shown
in the terminal where `exp` is running.

From now on everytime that you make a change to the source
code, figwheel will automatically reload the changes

beware that react native shows warning as small yellow boxes
at the bottom of the screen and error as a full red screen

## how to
add new assets or external modules
- require the module  
``` clj
(def cljs-logo (js/require "./assets/images/cljs.png"))
(def FontAwesome (js/require "@expo/vector-icons/FontAwesome"))
```
- reload simulator or device

## Notes
- make sure you disable live reload from the Developer Menu, also turn off Hot Module Reload.
Since Figwheel already does those.

- production build (generates `js/externs.js` and `main.js`)

``` shell
lein prod-build
```

- this project uses the fantastic [expo template](https://github.com/seantempesta/expo-cljs-template)
  from @seantempesta. In case of any doubt about the build process,
  please refer to his documentation
  
- check the [React Native](https://facebook.github.io/react-native/) docs when in doubt

- the [Expo](https://expo.io/) documentation will become your best friend

## known problems
- "unnable to resolve @expo/vector-icons/Feather ...". The solution is generally
 to install @expo/vector-icons, however, doing that makes the icons disappear
 and an [X] is shown instead, which is definitely not the idea. The "solution" so
 far is to go to the node_modules and remove the "Feather" import. This way
 the packager doesnt complain anymore and the icons are shown. 