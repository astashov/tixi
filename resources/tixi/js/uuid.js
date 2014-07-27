/*jslint bitwise: true */

goog.provide("tixi.uuid");

(function () {
    // From http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript
    function _p8(s) {
        var p = (Math.random().toString(16) + "000000000").substr(2, 8);
        return s ? "-" + p.substr(0, 4) + "-" + p.substr(4, 4) : p;
    }

    tixi.uuid.generate = function () {
        return _p8() + _p8(true) + _p8(true) + _p8();
    }
}());
