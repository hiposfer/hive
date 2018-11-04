# hive

Your go-to public transport routing app

## requirements

- [yarn](https://yarnpkg.com/lang/en/docs/install/)
- [lein](http://leiningen.org/#install)

## development

``` shell
# install js modules
npm install

# signup using exp CLI
npm run exp signup

# start the figwheel server and cljs repl 
lein figwheel
```

in another terminal
``` shell
## Start exponent server
npm run exp start
```

Once you have a figwheel and exp server running. Open the
expo app in your mobile device and scan the QR code shown
in the terminal where `exp` is running.

From now on everytime that you make a change to the source
code, figwheel will automatically reload the changes

Beware that react native shows warning as small yellow boxes
at the bottom of the screen and error as a full red screen

## how to
add new assets or external modules
- require the module  
``` clj
(def cljs-logo (js/require "./resources/images/cljs.png"))
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

- please check the [hive.rework.core](src/hive/rework/core.cljs) namespace for more information on how the
app is architectured.

## known problems
- we are currently using a very outdated version of react native, expo and the expo template
itself. We are working on it.
- sometimes the metro packager cannot properly find the source files. In such cases you see an
error in the terminal like `10:59:42 [exp] NotFoundError: Cannot find entry file target/expo/reagent/impl/template.js.js in any of the roots: ["/Users/Camilo/Proyectos/hive"]`.
To solve it, simply run `patch node_modules/metro/src/Server/index.js resources/metro.patch`
