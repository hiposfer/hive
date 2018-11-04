'use strict';

// cljsbuild adds a preamble mentioning goog so hack around it
// otherwise we would get a 'goog is undefined' error because
// require('./target/expo/env/index.js') uses google's closure imports
window.goog = {
    provide: function() {},
    require: function() {},
};

require('./target/expo/env/index.js');
