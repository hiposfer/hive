'use strict';

// cljsbuild adds a preamble mentioning goog so hack around it
window.goog = {
    provide: function() {},
    require: function() {},
};

require('./target/expo/env/index.js');
