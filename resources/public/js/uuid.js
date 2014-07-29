/*jslint bitwise: true */

goog.provide("tixi.uuid");

(function () {
    tixi.uuid.generate = function () {
        return Math.random().toString(16).substr(2, 8) + Math.random().toString(16).substr(2, 8);
    }
}());
